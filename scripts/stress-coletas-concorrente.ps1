[CmdletBinding()]
param(
    [string] $BaseUrl = "http://127.0.0.1:5012",
    [string] $EnvFile = (Join-Path (Split-Path -Parent $PSScriptRoot) ".env.development.local"),
    [int] $Concurrency = 20,
    [int] $DaysBack = 30,
    [int] $Limit = 50,
    [int] $Offset = 0,
    [ValidateSet("LegacyTabela", "Paginada")]
    [string] $EndpointMode = "LegacyTabela",
    [int] $TimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-DotEnv {
    param([Parameter(Mandatory = $true)][string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Arquivo de ambiente nao encontrado: $Path"
    }

    $values = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            return
        }

        $separator = $line.IndexOf("=")
        if ($separator -le 0) {
            return
        }

        $key = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()
        $values[$key] = $value
    }

    return $values
}

function Join-Url {
    param(
        [Parameter(Mandatory = $true)][string] $Root,
        [Parameter(Mandatory = $true)][string] $Path
    )

    return $Root.TrimEnd("/") + "/" + $Path.TrimStart("/")
}

function New-QueryString {
    param([Parameter(Mandatory = $true)][hashtable] $Params)

    $parts = foreach ($key in $Params.Keys) {
        $encodedKey = [System.Uri]::EscapeDataString([string] $key)
        $encodedValue = [System.Uri]::EscapeDataString([string] $Params[$key])
        "$encodedKey=$encodedValue"
    }

    return ($parts -join "&")
}

$envValues = Read-DotEnv -Path $EnvFile
$email = $envValues["ACESSO_USUARIO_SUPREMO_EMAIL"]
$password = $envValues["ACESSO_USUARIO_SUPREMO_SENHA_INICIAL"]

if ([string]::IsNullOrWhiteSpace($email) -or [string]::IsNullOrWhiteSpace($password)) {
    throw "Credenciais ACESSO_USUARIO_SUPREMO_EMAIL/SENHA_INICIAL ausentes no env."
}

$loginUri = Join-Url -Root $BaseUrl -Path "/api/auth/login"
$loginBody = @{
    email = $email
    senha = $password
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod -Uri $loginUri -Method Post -ContentType "application/json" -Body $loginBody -TimeoutSec $TimeoutSeconds
$token = $loginResponse.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login realizado, mas token JWT nao foi retornado."
}

$today = (Get-Date).Date
$dataInicio = $today.AddDays(-1 * $DaysBack).ToString("yyyy-MM-dd")
$dataFim = $today.ToString("yyyy-MM-dd")

if ($EndpointMode -eq "Paginada") {
    $endpointPath = "/api/painel/coletas/tabela/paginada"
    $pageNumber = [math]::Floor($Offset / [math]::Max(1, $Limit)) + 1
    $queryParams = [ordered] @{
        dataInicio = $dataInicio
        dataFim = $dataFim
        limit = $Limit
        offset = $Offset
        pagina = $pageNumber
        tamanhoPagina = $Limit
    }
} else {
    $endpointPath = "/api/painel/coletas/tabela"
    $queryParams = [ordered] @{
        dataInicio = $dataInicio
        dataFim = $dataFim
        limit = $Limit
        offset = $Offset
        limite = $Limit
    }
}

$targetUri = Join-Url -Root $BaseUrl -Path $endpointPath
$targetUri = $targetUri + "?" + (New-QueryString -Params $queryParams)

Import-Module ThreadJob -ErrorAction Stop

$barrierDir = Join-Path ([System.IO.Path]::GetTempPath()) ("coletas-stress-" + [System.Guid]::NewGuid().ToString("N"))
$null = New-Item -ItemType Directory -Path $barrierDir -Force
$gateFile = Join-Path $barrierDir "go"
$jobs = foreach ($id in 1..$Concurrency) {
    Start-ThreadJob -ThrottleLimit $Concurrency -Name ("coletas-stress-{0:D2}" -f $id) -ArgumentList $id, $targetUri, $token, $TimeoutSeconds, $barrierDir, $gateFile -ScriptBlock {
        param($RequestId, $Uri, $Token, $TimeoutSeconds, $BarrierDir, $GateFile)

        $readyFile = Join-Path $BarrierDir ("ready-{0:D2}" -f $RequestId)
        Set-Content -LiteralPath $readyFile -Value "ready" -NoNewline
        while (-not (Test-Path -LiteralPath $GateFile)) {
            Start-Sleep -Milliseconds 5
        }

        $client = [System.Net.Http.HttpClient]::new()
        $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)
        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $Uri)
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
        $request.Headers.Accept.ParseAdd("application/json")

        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $response = $client.SendAsync($request).GetAwaiter().GetResult()
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $sw.Stop()

            [pscustomobject] @{
                RequestId = $RequestId
                Status = [int] $response.StatusCode
                TimeMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                Bytes = [System.Text.Encoding]::UTF8.GetByteCount($body)
                Error = $null
            }
        } catch {
            $sw.Stop()
            [pscustomobject] @{
                RequestId = $RequestId
                Status = 0
                TimeMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                Bytes = 0
                Error = $_.Exception.Message
            }
        } finally {
            if ($null -ne $request) {
                $request.Dispose()
            }
            if ($null -ne $client) {
                $client.Dispose()
            }
        }
    }
}

$readyTimeout = [TimeSpan]::FromSeconds([math]::Max(30, $Concurrency * 2))
$readySw = [System.Diagnostics.Stopwatch]::StartNew()
while ((Get-ChildItem -LiteralPath $barrierDir -Filter "ready-*" -ErrorAction SilentlyContinue | Measure-Object).Count -lt $Concurrency) {
    if ($readySw.Elapsed -gt $readyTimeout) {
        Stop-Job -Job $jobs -ErrorAction SilentlyContinue
        Remove-Job -Job $jobs -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $barrierDir -Recurse -Force -ErrorAction SilentlyContinue
        throw "Timeout aguardando todas as threads ficarem prontas antes da largada."
    }
    Start-Sleep -Milliseconds 20
}

$startedAt = Get-Date
Set-Content -LiteralPath $gateFile -Value "go" -NoNewline
$null = Wait-Job -Job $jobs
$finishedAt = Get-Date
$results = Receive-Job -Job $jobs | Sort-Object RequestId
Remove-Job -Job $jobs
Remove-Item -LiteralPath $barrierDir -Recurse -Force -ErrorAction SilentlyContinue

$successfulTimes = @($results | Where-Object { $_.TimeMs -ge 0 } | Select-Object -ExpandProperty TimeMs)
$statusGroups = $results | Group-Object Status | Sort-Object { [int] $_.Name }

Write-Host "Stress Coletas Concorrente"
Write-Host "BaseUrl: $BaseUrl"
Write-Host "EndpointMode: $EndpointMode"
Write-Host "Endpoint: $targetUri"
Write-Host "Periodo: $dataInicio a $dataFim"
Write-Host "Concorrencia: $Concurrency"
Write-Host "Inicio: $($startedAt.ToString("yyyy-MM-dd HH:mm:ss.fff"))"
Write-Host "Fim: $($finishedAt.ToString("yyyy-MM-dd HH:mm:ss.fff"))"
Write-Host ""
Write-Host "Resultados por request:"
$results | Format-Table RequestId, Status, TimeMs, Bytes, Error -AutoSize

Write-Host ""
Write-Host "Resumo:"
Write-Host ("MinMs: {0:N2}" -f (($successfulTimes | Measure-Object -Minimum).Minimum))
Write-Host ("AvgMs: {0:N2}" -f (($successfulTimes | Measure-Object -Average).Average))
Write-Host ("MaxMs: {0:N2}" -f (($successfulTimes | Measure-Object -Maximum).Maximum))
Write-Host "Status:"
foreach ($group in $statusGroups) {
    Write-Host ("  {0}: {1}" -f $group.Name, $group.Count)
}
