[CmdletBinding()]
param(
    [string] $TaskName = "Dashboards SQL Backup",
    [string] $TaskPath = "\Dashboards\",
    [string] $At = "02:15",
    [string] $EnvFile = (Join-Path (Split-Path -Parent $PSScriptRoot) ".env"),
    [string] $BackupDirectory = "",
    [int] $RetentionDays = 14,
    [switch] $Compress
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$backupScript = Join-Path $PSScriptRoot "backup-dashboard-db.ps1"
if (-not (Test-Path -LiteralPath $backupScript)) {
    throw "Script de backup nao encontrado: $backupScript"
}
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Arquivo de ambiente nao encontrado: $EnvFile"
}

$backupScriptFull = [System.IO.Path]::GetFullPath($backupScript)
$envFileFull = [System.IO.Path]::GetFullPath($EnvFile)
$arguments = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", ('"{0}"' -f $backupScriptFull),
    "-EnvFile", ('"{0}"' -f $envFileFull),
    "-RetentionDays", $RetentionDays
)

if (-not [string]::IsNullOrWhiteSpace($BackupDirectory)) {
    $backupDirectoryFull = [System.IO.Path]::GetFullPath($BackupDirectory)
    $arguments += @("-BackupDirectory", ('"{0}"' -f $backupDirectoryFull))
}
if ($Compress) {
    $arguments += "-Compress"
}

$time = [DateTime]::ParseExact($At, "HH:mm", [Globalization.CultureInfo]::InvariantCulture)
$powershellExe = (Get-Command powershell.exe -ErrorAction Stop).Source
$action = New-ScheduledTaskAction -Execute $powershellExe -Argument ($arguments -join " ")
$trigger = New-ScheduledTaskTrigger -Daily -At $time
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Hours 3)
$principal = New-ScheduledTaskPrincipal `
    -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive `
    -RunLevel Highest

Register-ScheduledTask `
    -TaskName $TaskName `
    -TaskPath $TaskPath `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description "Backup diario do banco DASHBOARDS usando scripts versionados do portal." `
    -Force | Out-Null

Write-Host "[OK] Tarefa agendada registrada: $TaskPath$TaskName ($At)"
