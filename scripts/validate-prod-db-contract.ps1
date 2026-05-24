$ErrorActionPreference = 'Stop'

function Resolve-SqlServer {
    param([Parameter(Mandatory = $true)][string] $DbUrl)

    if ($DbUrl -match 'jdbc:sqlserver://([^;]+)') {
        return ($Matches[1] -replace ':(\d+)$', ',$1')
    }

    return 'localhost,1433'
}

function Resolve-SqlDatabase {
    param([Parameter(Mandatory = $true)][string] $DbUrl)

    if ($DbUrl -match '(?i)databaseName=([^;]+)') {
        return $Matches[1]
    }

    if ($DbUrl -match '(?i)database=([^;]+)') {
        return $Matches[1]
    }

    return 'DASHBOARDS'
}

if ([string]::IsNullOrWhiteSpace($env:DB_URL)) {
    Write-Host '[ERRO] DB_URL nao definido em dashboards\.env.'
    exit 1
}

if ([string]::IsNullOrWhiteSpace($env:DB_USER) -or [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    Write-Host '[ERRO] DB_USER/DB_PASSWORD nao definidos em dashboards\.env.'
    exit 1
}

$server = Resolve-SqlServer -DbUrl $env:DB_URL
$database = Resolve-SqlDatabase -DbUrl $env:DB_URL

$query = @'
SET NOCOUNT ON;

IF DB_ID(N'ETL_SISTEMA') IS NULL
    THROW 52100, 'Contrato invalido: database ETL_SISTEMA ausente.', 1;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_localizacao_cargas_powerbi', N'V') IS NULL
    THROW 52101, 'Contrato invalido: ETL_SISTEMA.dbo.vw_localizacao_cargas_powerbi ausente.', 1;

IF OBJECT_ID(N'dbo.vw_localizacao_cargas_powerbi', N'V') IS NULL
    THROW 52102, 'Contrato invalido: dbo.vw_localizacao_cargas_powerbi ausente em DASHBOARDS.', 1;

DECLARE @esperadas TABLE (ordinal INT NOT NULL PRIMARY KEY, nome SYSNAME NOT NULL);
INSERT INTO @esperadas (ordinal, nome)
VALUES
    (1, N'Hora (Solicitacao)'),
    (2, N'N' + NCHAR(176) + N' Minuta'),
    (3, N'Tipo'),
    (4, N'Data do frete'),
    (5, N'Volumes'),
    (6, N'Peso Taxado'),
    (7, N'Peso Taxado Decimal'),
    (8, N'Valor NF'),
    (9, N'Valor NF Decimal'),
    (10, N'Valor Frete'),
    (11, N'Tipo Servi' + NCHAR(231) + N'o'),
    (12, N'Filial Emissora'),
    (13, N'Previs' + NCHAR(227) + N'o Entrega/Previs' + NCHAR(227) + N'o de entrega'),
    (14, N'Regi' + NCHAR(227) + N'o Destino'),
    (15, N'Filial Destino'),
    (16, N'Respons' + NCHAR(225) + N'vel pela Regi' + NCHAR(227) + N'o de Destino'),
    (17, N'Sigla Respons' + NCHAR(225) + N'vel Regi' + NCHAR(227) + N'o Destino'),
    (18, N'Classifica' + NCHAR(231) + NCHAR(227) + N'o'),
    (19, N'Status Carga'),
    (20, N'Status Normalizado'),
    (21, N'Status Terminal'),
    (22, N'Cancelado Flag'),
    (23, N'Filial Atual'),
    (24, N'Regi' + NCHAR(227) + N'o Origem'),
    (25, N'Filial Origem'),
    (26, N'Localiza' + NCHAR(231) + NCHAR(227) + N'o Atual'),
    (27, N'Hash Localiza' + NCHAR(231) + NCHAR(227) + N'o'),
    (28, N'Metadata'),
    (29, N'Data de extracao');

IF EXISTS (
    SELECT 1
    FROM @esperadas e
    WHERE NOT EXISTS (
        SELECT 1
        FROM ETL_SISTEMA.sys.columns c
        JOIN ETL_SISTEMA.sys.objects o ON o.object_id = c.object_id
        JOIN ETL_SISTEMA.sys.schemas s ON s.schema_id = o.schema_id
        WHERE s.name = N'dbo'
          AND o.name = N'vw_localizacao_cargas_powerbi'
          AND c.name = e.nome
    )
)
    THROW 52103, 'Contrato invalido: view ETL de localizacao sem colunas governadas. Aplique as migrations do ETL antes do Dashboard.', 1;

IF EXISTS (
    SELECT 1
    FROM @esperadas e
    LEFT JOIN sys.columns c
      ON c.object_id = OBJECT_ID(N'dbo.vw_localizacao_cargas_powerbi', N'V')
     AND c.column_id = e.ordinal
    WHERE c.name IS NULL OR c.name <> e.nome
) OR (
    SELECT COUNT(1)
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.vw_localizacao_cargas_powerbi', N'V')
) <> (SELECT COUNT(1) FROM @esperadas)
BEGIN
    THROW 52104, 'Contrato invalido: wrapper dbo.vw_localizacao_cargas_powerbi desatualizado. Reaplique V019__sincronizar_view_localizacao_cargas_etl.sql no banco DASHBOARDS.', 1;
END;

PRINT '[OK] Contrato de localizacao de cargas validado.';
'@

$args = @(
    '-S', $server,
    '-d', $database,
    '-U', $env:DB_USER,
    '-P', $env:DB_PASSWORD,
    '-C',
    '-b',
    '-Q', $query
)

$output = & SQLCMD.EXE @args 2>&1
$exitCode = $LASTEXITCODE
$output | ForEach-Object { Write-Host $_ }

if ($exitCode -ne 0) {
    exit $exitCode
}
