[CmdletBinding()]
param(
    [string] $EnvFile = (Join-Path (Split-Path -Parent $PSScriptRoot) ".env"),
    [string] $BackupDirectory = "",
    [int] $RetentionDays = -1,
    [switch] $Compress
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-DotEnv {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Arquivo de ambiente nao encontrado: $Path"
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -le 0) {
            continue
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$key] = $value
    }
    return $values
}

function Get-ConfigValue {
    param(
        [hashtable] $Values,
        [string] $Name,
        [string] $DefaultValue = ""
    )

    if ($Values.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace([string] $Values[$Name])) {
        return [string] $Values[$Name]
    }
    $envValue = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($envValue)) {
        return $envValue
    }
    return $DefaultValue
}

function Parse-JdbcSqlServerUrl {
    param([string] $DbUrl)

    if ($DbUrl -notmatch "^jdbc:sqlserver://([^;]+)(?:;(.*))?$") {
        throw "DB_URL deve usar o formato jdbc:sqlserver://host:porta;databaseName=DASHBOARDS;..."
    }

    $server = $matches[1]
    $params = @{}
    if ($matches.Count -ge 3 -and -not [string]::IsNullOrWhiteSpace($matches[2])) {
        foreach ($part in ($matches[2] -split ";")) {
            $separator = $part.IndexOf("=")
            if ($separator -le 0) {
                continue
            }
            $key = $part.Substring(0, $separator).Trim().ToLowerInvariant()
            $value = $part.Substring($separator + 1).Trim()
            $params[$key] = $value
        }
    }

    $database = ""
    foreach ($key in @("databasename", "database")) {
        if ($params.ContainsKey($key)) {
            $database = [string] $params[$key]
            break
        }
    }
    if ([string]::IsNullOrWhiteSpace($database)) {
        throw "DB_URL precisa informar databaseName ou database."
    }

    [pscustomobject]@{
        Server = $server
        Database = $database
        Encrypt = ($params.ContainsKey("encrypt") -and ([string] $params["encrypt"]).Equals("true", [StringComparison]::OrdinalIgnoreCase))
        TrustServerCertificate = ($params.ContainsKey("trustservercertificate") -and ([string] $params["trustservercertificate"]).Equals("true", [StringComparison]::OrdinalIgnoreCase))
    }
}

function Quote-SqlIdentifier {
    param([string] $Value)
    return "[" + $Value.Replace("]", "]]") + "]"
}

function Quote-SqlLiteral {
    param([string] $Value)
    return "N'" + $Value.Replace("'", "''") + "'"
}

function Get-SafeFileNamePart {
    param([string] $Value)
    $invalidChars = [System.IO.Path]::GetInvalidFileNameChars()
    $result = $Value
    foreach ($char in $invalidChars) {
        $result = $result.Replace([string] $char, "_")
    }
    return $result
}

$rootDir = Split-Path -Parent $PSScriptRoot
$envValues = Read-DotEnv -Path $EnvFile
$dbUrl = Get-ConfigValue -Values $envValues -Name "DB_URL"
$dbUser = Get-ConfigValue -Values $envValues -Name "DB_USER"
$dbPassword = Get-ConfigValue -Values $envValues -Name "DB_PASSWORD"

if ([string]::IsNullOrWhiteSpace($dbUrl) -or [string]::IsNullOrWhiteSpace($dbUser) -or [string]::IsNullOrWhiteSpace($dbPassword)) {
    throw "DB_URL, DB_USER e DB_PASSWORD sao obrigatorios para backup."
}

$parsedUrl = Parse-JdbcSqlServerUrl -DbUrl $dbUrl
if ($parsedUrl.Database -notin @("DASHBOARDS", "DASHBOARDS_DEV")) {
    throw "Backup bloqueado: banco alvo '$($parsedUrl.Database)' nao pertence ao portal Dashboards."
}

if ([string]::IsNullOrWhiteSpace($BackupDirectory)) {
    $BackupDirectory = Get-ConfigValue -Values $envValues -Name "DASHBOARDS_BACKUP_DIR" -DefaultValue (Join-Path $rootDir "backups\sqlserver")
}
if ($RetentionDays -lt 0) {
    $retentionValue = Get-ConfigValue -Values $envValues -Name "DASHBOARDS_BACKUP_RETENTION_DAYS" -DefaultValue "14"
    $RetentionDays = [int] $retentionValue
}

$backupRoot = [System.IO.Path]::GetFullPath($BackupDirectory)
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$safeDatabase = Get-SafeFileNamePart -Value $parsedUrl.Database
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss")
$backupFile = Join-Path $backupRoot ("dashboards-{0}-{1}.bak" -f $safeDatabase, $timestamp)

Add-Type -AssemblyName System.Data
$builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder["Data Source"] = $parsedUrl.Server
$builder["Initial Catalog"] = "master"
$builder["User ID"] = $dbUser
$builder["Password"] = $dbPassword
$builder["Encrypt"] = [bool] $parsedUrl.Encrypt
$builder["TrustServerCertificate"] = [bool] $parsedUrl.TrustServerCertificate
$builder["Connect Timeout"] = 30
$builder["Application Name"] = "DashboardsBackup"

$options = @("COPY_ONLY", "CHECKSUM", "INIT", "STATS = 10")
if ($Compress) {
    $options += "COMPRESSION"
}

$backupSql = "BACKUP DATABASE $(Quote-SqlIdentifier $parsedUrl.Database) TO DISK = $(Quote-SqlLiteral $backupFile) WITH $($options -join ', ');"
$verifySql = "RESTORE VERIFYONLY FROM DISK = $(Quote-SqlLiteral $backupFile) WITH CHECKSUM;"

$connection = New-Object System.Data.SqlClient.SqlConnection $builder.ConnectionString
try {
    $connection.Open()

    $command = $connection.CreateCommand()
    $command.CommandTimeout = 0
    $command.CommandText = $backupSql
    [void] $command.ExecuteNonQuery()

    $verifyCommand = $connection.CreateCommand()
    $verifyCommand.CommandTimeout = 0
    $verifyCommand.CommandText = $verifySql
    [void] $verifyCommand.ExecuteNonQuery()
} finally {
    $connection.Dispose()
}

if ($RetentionDays -gt 0) {
    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    $trimChars = @([char] [System.IO.Path]::DirectorySeparatorChar, [char] [System.IO.Path]::AltDirectorySeparatorChar)
    $rootWithSeparator = $backupRoot.TrimEnd($trimChars) + [System.IO.Path]::DirectorySeparatorChar
    Get-ChildItem -LiteralPath $backupRoot -Filter ("dashboards-{0}-*.bak" -f $safeDatabase) -File |
        Where-Object { $_.LastWriteTime -lt $cutoff } |
        ForEach-Object {
            $candidate = [System.IO.Path]::GetFullPath($_.FullName)
            if (-not $candidate.StartsWith($rootWithSeparator, [StringComparison]::OrdinalIgnoreCase)) {
                throw "Retencao bloqueada: caminho fora do diretorio de backup: $candidate"
            }
            Remove-Item -LiteralPath $candidate -Force
        }
}

Write-Host "[OK] Backup $($parsedUrl.Database) validado em $backupFile"
