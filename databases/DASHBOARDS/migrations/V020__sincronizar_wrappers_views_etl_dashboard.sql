IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 52000, 'Database ETL_SISTEMA nao encontrada para sincronizar wrappers das views ETL do Dashboard.', 1;
END;

DECLARE @views TABLE (nome SYSNAME NOT NULL PRIMARY KEY);

INSERT INTO @views (nome)
VALUES
    (N'vw_bi_monitoramento'),
    (N'vw_coletas_powerbi'),
    (N'vw_contas_a_pagar_powerbi'),
    (N'vw_cotacoes_powerbi'),
    (N'vw_dim_clientes'),
    (N'vw_dim_filiais'),
    (N'vw_dim_motoristas'),
    (N'vw_dim_planocontas'),
    (N'vw_dim_usuarios'),
    (N'vw_dim_veiculos'),
    (N'vw_faturas_graphql_powerbi'),
    (N'vw_inventario_powerbi'),
    (N'vw_manifestos_powerbi'),
    (N'vw_sinistros_powerbi');

IF EXISTS (
    SELECT 1
    FROM @views v
    WHERE OBJECT_ID(N'ETL_SISTEMA.dbo.' + v.nome, N'V') IS NULL
)
BEGIN
    DECLARE @viewsFaltantes NVARCHAR(MAX);

    SELECT @viewsFaltantes = STRING_AGG(CONVERT(NVARCHAR(MAX), v.nome), N', ')
    FROM @views v
    WHERE OBJECT_ID(N'ETL_SISTEMA.dbo.' + v.nome, N'V') IS NULL;

    DECLARE @mensagemViews NVARCHAR(2048) = N'Views ETL_SISTEMA ausentes para wrappers do Dashboard: '
        + COALESCE(@viewsFaltantes, N'(nao identificadas)')
        + N'. Aplique as migrations/views do projeto etl-extracao-dados antes desta migration.';

    THROW 52001, @mensagemViews, 1;
END;

IF NOT EXISTS (
    SELECT 1
    FROM ETL_SISTEMA.sys.columns c
    JOIN ETL_SISTEMA.sys.objects o ON o.object_id = c.object_id
    JOIN ETL_SISTEMA.sys.schemas s ON s.schema_id = o.schema_id
    JOIN ETL_SISTEMA.sys.types t ON t.user_type_id = c.user_type_id
    WHERE s.name = N'dbo'
      AND o.name = N'vw_coletas_powerbi'
      AND c.name = N'Solicitacao'
      AND t.name IN (N'date', N'datetime', N'datetime2', N'datetimeoffset', N'smalldatetime')
)
BEGIN
    THROW 52002, 'Contrato invalido: ETL_SISTEMA.dbo.vw_coletas_powerbi.[Solicitacao] deve ser tipo de data nativo. Corrija o ETL; nao use conversao dinamica no Dashboard.', 1;
END;

DECLARE @nome SYSNAME;
DECLARE @sql NVARCHAR(MAX);
DECLARE @objetoLocal NVARCHAR(300);

DECLARE wrappers_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT nome
    FROM @views
    ORDER BY nome;

OPEN wrappers_cursor;
FETCH NEXT FROM wrappers_cursor INTO @nome;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'CREATE OR ALTER VIEW dbo.' + QUOTENAME(@nome) + N'
AS
SELECT *
FROM [ETL_SISTEMA].dbo.' + QUOTENAME(@nome) + N';';

    EXEC sys.sp_executesql @sql;

    SET @objetoLocal = N'dbo.' + QUOTENAME(@nome);
    EXEC sys.sp_refreshview @objetoLocal;

    FETCH NEXT FROM wrappers_cursor INTO @nome;
END;

CLOSE wrappers_cursor;
DEALLOCATE wrappers_cursor;
