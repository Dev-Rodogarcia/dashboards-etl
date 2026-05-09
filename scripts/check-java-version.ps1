[CmdletBinding()]
param(
    [int]$MinimumMajor = 17
)

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) {
    Write-Host "[ERRO] Java nao encontrado no PATH."
    exit 1
}

$versionOutput = & java -version 2>&1 | ForEach-Object { $_.ToString() }
$firstLine = $versionOutput | Select-Object -First 1

if (-not $firstLine -or $firstLine -notmatch '(?<version>\d+(?:\.\d+)*(?:_\d+)?)') {
    Write-Host "[ERRO] Nao foi possivel identificar a versao do Java."
    if ($firstLine) {
        Write-Host "[INFO] Saida recebida: $firstLine"
    }
    exit 1
}

$version = $Matches.version
if ($version -like '1.*') {
    $major = [int]($version.Split('.')[1])
} else {
    $major = [int]($version.Split('.')[0])
}

if ($major -lt $MinimumMajor) {
    Write-Host "[ERRO] Java $MinimumMajor+ requerido. Encontrado Java $version em $($javaCommand.Source)."
    if ($env:JAVA_HOME) {
        Write-Host "[INFO] JAVA_HOME atual: $env:JAVA_HOME"
        Write-Host "[INFO] Confirme se %JAVA_HOME%\bin vem antes do Java 8 no PATH."
    } else {
        Write-Host "[INFO] Configure JAVA_HOME para um JDK $MinimumMajor+."
    }
    exit 2
}

Write-Host "[OK] Java $version detectado em $($javaCommand.Source)."
exit 0
