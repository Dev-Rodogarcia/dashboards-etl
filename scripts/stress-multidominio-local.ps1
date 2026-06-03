[CmdletBinding()]
param(
    [string] $BaseUrl = "http://127.0.0.1:5011",
    [string] $EnvFile = "",
    [int] $Concurrency = 20,
    [string] $DataInicio = "2025-12-05",
    [string] $DataFim = "2026-06-03",
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
        $value = $line.Substring($separator + 1).Trim().Trim([char]34).Trim([char]39)
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

if ($Concurrency -lt 1) {
    throw "Concurrency deve ser maior que zero."
}

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $scriptDir = if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) { Split-Path -Parent $MyInvocation.MyCommand.Path } else { $PSScriptRoot }
    $EnvFile = Join-Path (Split-Path -Parent $scriptDir) ".env.development.local"
}

$endpoints = @(
    [pscustomobject] @{
        Key = "fretes_tabela_paginada"
        Path = "/api/painel/fretes/tabela/paginada"
        QueryString = New-QueryString -Params ([ordered] @{
            pagina = 1
            tamanhoPagina = 50
            dataInicio = $DataInicio
            dataFim = $DataFim
        })
    },
    [pscustomobject] @{
        Key = "faturas_por_cliente_aging"
        Path = "/api/painel/faturas-por-cliente/aging"
        QueryString = New-QueryString -Params ([ordered] @{
            dataInicio = $DataInicio
            dataFim = $DataFim
        })
    },
    [pscustomobject] @{
        Key = "utilizacao_coletores_ranking"
        Path = "/api/painel/indicadores-gestao-a-vista/utilizacao-coletores/ranking"
        QueryString = New-QueryString -Params ([ordered] @{
            dataInicio = $DataInicio
            dataFim = $DataFim
        })
    },
    [pscustomobject] @{
        Key = "etl_saude"
        Path = "/api/painel/etl-saude"
        QueryString = New-QueryString -Params ([ordered] @{
            dataInicio = $DataInicio
            dataFim = $DataFim
        })
    }
)

if (-not (Get-Command Start-ThreadJob -ErrorAction SilentlyContinue)) {
    throw "Start-ThreadJob nao esta disponivel neste PowerShell. Execute com pwsh 7+ ou instale o modulo ThreadJob."
}
Import-Module ThreadJob -ErrorAction Stop

$envValues = Read-DotEnv -Path $EnvFile
$email = [string] $envValues["ACESSO_USUARIO_SUPREMO_EMAIL"]
$password = [string] $envValues["ACESSO_USUARIO_SUPREMO_SENHA_INICIAL"]

if ([string]::IsNullOrWhiteSpace($email) -or [string]::IsNullOrWhiteSpace($password)) {
    throw "Credenciais ACESSO_USUARIO_SUPREMO_EMAIL/SENHA_INICIAL ausentes no env."
}

$loginUri = Join-Url -Root $BaseUrl -Path "/api/auth/login"
$loginBody = @{
    email = $email
    senha = $password
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod -Uri $loginUri -Method Post -ContentType "application/json" -Body $loginBody -TimeoutSec $TimeoutSeconds
$token = [string] $loginResponse.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login realizado, mas token JWT nao foi retornado."
}

$barrierDir = Join-Path ([System.IO.Path]::GetTempPath()) ("dashboard-multidominio-stress-" + [System.Guid]::NewGuid().ToString("N"))
$null = New-Item -ItemType Directory -Path $barrierDir -Force
$gateFile = Join-Path $barrierDir "go"

$jobs = foreach ($id in 1..$Concurrency) {
    Start-ThreadJob -ThrottleLimit $Concurrency -Name ("stress-multidominio-{0:D2}" -f $id) -ArgumentList $id, $BaseUrl, $endpoints, $token, $TimeoutSeconds, $barrierDir, $gateFile -ScriptBlock {
        param($ThreadId, $RootUrl, $Targets, $JwtToken, $RequestTimeoutSeconds, $BarrierDir, $GateFile)

        $readyFile = Join-Path $BarrierDir ("ready-{0:D2}" -f $ThreadId)
        Set-Content -LiteralPath $readyFile -Value "ready" -NoNewline
        while (-not (Test-Path -LiteralPath $GateFile)) {
            Start-Sleep -Milliseconds 5
        }

        $client = [System.Net.Http.HttpClient]::new()
        $client.Timeout = [TimeSpan]::FromSeconds($RequestTimeoutSeconds)
        $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $JwtToken)
        $client.DefaultRequestHeaders.Accept.ParseAdd("application/json")
        $client.DefaultRequestHeaders.UserAgent.ParseAdd("codex-local-stress/1.0")

        try {
            $targetCount = $Targets.Count
            for ($i = 0; $i -lt $targetCount; $i++) {
                $targetIndex = ($i + (($ThreadId - 1) % $targetCount)) % $targetCount
                $target = $Targets[$targetIndex]
                $uri = $RootUrl.TrimEnd("/") + $target.Path + "?" + $target.QueryString

                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                try {
                    $response = $client.GetAsync($uri).GetAwaiter().GetResult()
                    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                    $sw.Stop()

                    [pscustomobject] @{
                        ThreadId = $ThreadId
                        Endpoint = $target.Key
                        Path = $target.Path
                        Status = [int] $response.StatusCode
                        TimeMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                        Bytes = [System.Text.Encoding]::UTF8.GetByteCount($body)
                        Error = $null
                    }
                } catch {
                    $sw.Stop()
                    [pscustomobject] @{
                        ThreadId = $ThreadId
                        Endpoint = $target.Key
                        Path = $target.Path
                        Status = 0
                        TimeMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                        Bytes = 0
                        Error = $_.Exception.Message
                    }
                }
            }
        } finally {
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
$results = @(Receive-Job -Job $jobs)
Remove-Job -Job $jobs
Remove-Item -LiteralPath $barrierDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Stress Multi-Dominio Local"
Write-Host "BaseUrl: $BaseUrl"
Write-Host "Periodo: $DataInicio a $DataFim"
Write-Host "Threads reais: $Concurrency"
Write-Host "Requests por endpoint: $Concurrency"
Write-Host "Requests totais: $($results.Count)"
Write-Host "Inicio: $($startedAt.ToString("yyyy-MM-dd HH:mm:ss.fff"))"
Write-Host "Fim: $($finishedAt.ToString("yyyy-MM-dd HH:mm:ss.fff"))"
Write-Host ""
Write-Host "Resultados por request:"
$results | Sort-Object Endpoint, ThreadId | Format-Table ThreadId, Endpoint, Status, TimeMs, Bytes, Error -AutoSize

Write-Host ""
Write-Host "Resumo por endpoint:"
$summary = foreach ($group in ($results | Group-Object Endpoint | Sort-Object Name)) {
    $rows = @($group.Group)
    $times = @($rows | Where-Object { $_.TimeMs -ge 0 } | Select-Object -ExpandProperty TimeMs)
    $statusCounts = ($rows | Group-Object Status | Sort-Object { [int] $_.Name } | ForEach-Object { "{0}:{1}" -f $_.Name, $_.Count }) -join ", "

    [pscustomobject] @{
        Endpoint = $group.Name
        Requests = $rows.Count
        MinMs = [math]::Round((($times | Measure-Object -Minimum).Minimum), 2)
        AvgMs = [math]::Round((($times | Measure-Object -Average).Average), 2)
        MaxMs = [math]::Round((($times | Measure-Object -Maximum).Maximum), 2)
        StatusCounts = $statusCounts
    }
}

$summary | Format-Table Endpoint, Requests, MinMs, AvgMs, MaxMs, StatusCounts -AutoSize
