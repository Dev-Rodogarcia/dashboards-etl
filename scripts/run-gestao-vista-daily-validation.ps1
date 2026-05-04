param(
    [string] $DataInicio = "2026-03-01",
    [string] $DataFim = "2026-03-31",
    [string] $ApiBaseUrl = "http://localhost:5010",
    [string] $Xlsx = "Análise - Divergências - Indicadores Projeto Gestão a Vista Operacional.xlsx"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    node scripts\validate-gestao-vista-xlsx-vs-dashboard.mjs `
        --xlsx="$Xlsx" `
        --dataInicio="$DataInicio" `
        --dataFim="$DataFim" `
        --apiBaseUrl="$ApiBaseUrl"

    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
