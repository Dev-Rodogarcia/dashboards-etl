:setvar SourceDb ETL_SISTEMA
:setvar TargetDb DASHBOARDS
:setvar AppLogin usuario_etl

SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE [master];
GO

IF DB_ID(N'$(SourceDb)') IS NULL
BEGIN
    THROW 51000, 'Database de origem não encontrada.', 1;
END;

IF DB_ID(N'$(TargetDb)') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [$(TargetDb)]');
END;
GO

USE [$(SourceDb)];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

DECLARE @TargetDb SYSNAME = N'$(TargetDb)';
DECLARE @SourceDb SYSNAME = DB_NAME();
DECLARE @sql NVARCHAR(MAX);

IF SCHEMA_ID(N'acesso') IS NULL
BEGIN
    THROW 51001, 'Schema acesso não encontrado na origem.', 1;
END;

DECLARE @schemas TABLE (schema_name SYSNAME PRIMARY KEY);

INSERT INTO @schemas (schema_name)
SELECT DISTINCT s.name
FROM sys.tables t
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE s.name = N'acesso';

IF NOT EXISTS (SELECT 1 FROM @schemas)
BEGIN
    THROW 51002, 'Nenhuma tabela de acesso encontrada na origem.', 1;
END;

DECLARE @schemaName SYSNAME;
DECLARE schema_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT schema_name FROM @schemas ORDER BY schema_name;

OPEN schema_cursor;
FETCH NEXT FROM schema_cursor INTO @schemaName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF SCHEMA_ID(N''' + REPLACE(@schemaName, '''', '''''') + N''') IS NULL
            EXEC(N''CREATE SCHEMA ' + QUOTENAME(@schemaName) + N''');';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM schema_cursor INTO @schemaName;
END;

CLOSE schema_cursor;
DEALLOCATE schema_cursor;

DECLARE @tables TABLE (
    object_id INT PRIMARY KEY,
    schema_name SYSNAME NOT NULL,
    table_name SYSNAME NOT NULL
);

INSERT INTO @tables (object_id, schema_name, table_name)
SELECT t.object_id, s.name, t.name
FROM sys.tables t
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE s.name IN (SELECT schema_name FROM @schemas)
ORDER BY s.name, t.name;

DECLARE @objectId INT;
DECLARE @tableName SYSNAME;
DECLARE table_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT object_id, schema_name, table_name
FROM @tables
ORDER BY schema_name, table_name;

OPEN table_cursor;
FETCH NEXT FROM table_cursor INTO @objectId, @schemaName, @tableName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'
        IF NOT EXISTS (
            SELECT 1
            FROM ' + QUOTENAME(@TargetDb) + N'.sys.tables t
            INNER JOIN ' + QUOTENAME(@TargetDb) + N'.sys.schemas s ON s.schema_id = t.schema_id
            WHERE s.name = N''' + REPLACE(@schemaName, '''', '''''') + N'''
              AND t.name = N''' + REPLACE(@tableName, '''', '''''') + N'''
        )
        BEGIN
            SELECT TOP (0) *
            INTO ' + QUOTENAME(@TargetDb) + N'.' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N'
            FROM ' + QUOTENAME(@SourceDb) + N'.' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N';
        END;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM table_cursor INTO @objectId, @schemaName, @tableName;
END;

CLOSE table_cursor;
DEALLOCATE table_cursor;

DECLARE @constraintName SYSNAME;
DECLARE @definition NVARCHAR(MAX);
DECLARE @columnName SYSNAME;

DECLARE default_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT s.name, t.name, c.name, dc.name, dc.definition
FROM sys.default_constraints dc
INNER JOIN sys.tables t ON t.object_id = dc.parent_object_id
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
INNER JOIN sys.columns c ON c.object_id = t.object_id AND c.column_id = dc.parent_column_id
WHERE t.object_id IN (SELECT object_id FROM @tables)
ORDER BY s.name, t.name, dc.name;

OPEN default_cursor;
FETCH NEXT FROM default_cursor INTO @schemaName, @tableName, @columnName, @constraintName, @definition;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'') IS NOT NULL
           AND NOT EXISTS (
                SELECT 1 FROM sys.default_constraints
                WHERE name = N''' + REPLACE(@constraintName, '''', '''''') + N'''
                  AND parent_object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'')
           )
        BEGIN
            ALTER TABLE ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N'
            ADD CONSTRAINT ' + QUOTENAME(@constraintName) + N' DEFAULT ' + @definition + N' FOR ' + QUOTENAME(@columnName) + N';
        END;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM default_cursor INTO @schemaName, @tableName, @columnName, @constraintName, @definition;
END;

CLOSE default_cursor;
DEALLOCATE default_cursor;

DECLARE @constraintType CHAR(2);
DECLARE @indexId INT;
DECLARE @cols NVARCHAR(MAX);
DECLARE @clustered NVARCHAR(20);

DECLARE key_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT s.name, t.name, kc.name, kc.type, kc.unique_index_id
FROM sys.key_constraints kc
INNER JOIN sys.tables t ON t.object_id = kc.parent_object_id
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE t.object_id IN (SELECT object_id FROM @tables)
ORDER BY CASE kc.type WHEN 'PK' THEN 0 ELSE 1 END, s.name, t.name, kc.name;

OPEN key_cursor;
FETCH NEXT FROM key_cursor INTO @schemaName, @tableName, @constraintName, @constraintType, @indexId;

WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @objectId = OBJECT_ID(QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName));

    SELECT @cols = STRING_AGG(
        QUOTENAME(c.name) + CASE WHEN ic.is_descending_key = 1 THEN N' DESC' ELSE N' ASC' END,
        N', '
    ) WITHIN GROUP (ORDER BY ic.key_ordinal)
    FROM sys.index_columns ic
    INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE ic.object_id = @objectId
      AND ic.index_id = @indexId
      AND ic.key_ordinal > 0;

    SELECT @clustered = CASE WHEN i.type_desc = 'CLUSTERED' THEN N'CLUSTERED' ELSE N'NONCLUSTERED' END
    FROM sys.indexes i
    WHERE i.object_id = @objectId
      AND i.index_id = @indexId;

    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'') IS NOT NULL
           AND NOT EXISTS (
                SELECT 1 FROM sys.key_constraints
                WHERE name = N''' + REPLACE(@constraintName, '''', '''''') + N'''
                  AND parent_object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'')
           )
        BEGIN
            ALTER TABLE ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N'
            ADD CONSTRAINT ' + QUOTENAME(@constraintName) + N' ' +
            CASE WHEN @constraintType = 'PK' THEN N'PRIMARY KEY ' ELSE N'UNIQUE ' END +
            @clustered + N' (' + @cols + N');
        END;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM key_cursor INTO @schemaName, @tableName, @constraintName, @constraintType, @indexId;
END;

CLOSE key_cursor;
DEALLOCATE key_cursor;

DECLARE check_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT s.name, t.name, cc.name, cc.definition
FROM sys.check_constraints cc
INNER JOIN sys.tables t ON t.object_id = cc.parent_object_id
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE t.object_id IN (SELECT object_id FROM @tables)
ORDER BY s.name, t.name, cc.name;

OPEN check_cursor;
FETCH NEXT FROM check_cursor INTO @schemaName, @tableName, @constraintName, @definition;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'') IS NOT NULL
           AND NOT EXISTS (
                SELECT 1 FROM sys.check_constraints
                WHERE name = N''' + REPLACE(@constraintName, '''', '''''') + N'''
                  AND parent_object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'')
           )
        BEGIN
            ALTER TABLE ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N'
            WITH CHECK ADD CONSTRAINT ' + QUOTENAME(@constraintName) + N' CHECK ' + @definition + N';
        END;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM check_cursor INTO @schemaName, @tableName, @constraintName, @definition;
END;

CLOSE check_cursor;
DEALLOCATE check_cursor;

DECLARE @hasIdentity BIT;
DECLARE @pkPredicate NVARCHAR(MAX);
DECLARE @targetHasRowsSql NVARCHAR(MAX);

DECLARE copy_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT object_id, schema_name, table_name
FROM @tables
ORDER BY schema_name, table_name;

OPEN copy_cursor;
FETCH NEXT FROM copy_cursor INTO @objectId, @schemaName, @tableName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @cols = STRING_AGG(QUOTENAME(c.name), N', ') WITHIN GROUP (ORDER BY c.column_id)
    FROM sys.columns c
    WHERE c.object_id = @objectId
      AND c.is_computed = 0;

    SELECT @hasIdentity = CASE WHEN EXISTS (
        SELECT 1 FROM sys.identity_columns WHERE object_id = @objectId
    ) THEN 1 ELSE 0 END;

    SELECT @pkPredicate = STRING_AGG(
        N'tgt.' + QUOTENAME(c.name) + N' = src.' + QUOTENAME(c.name),
        N' AND '
    ) WITHIN GROUP (ORDER BY ic.key_ordinal)
    FROM sys.key_constraints kc
    INNER JOIN sys.index_columns ic
        ON ic.object_id = kc.parent_object_id
       AND ic.index_id = kc.unique_index_id
       AND ic.key_ordinal > 0
    INNER JOIN sys.columns c
        ON c.object_id = ic.object_id
       AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = @objectId
      AND kc.type = 'PK';

    SET @targetHasRowsSql = CASE
        WHEN @pkPredicate IS NULL THEN
            N'IF NOT EXISTS (SELECT 1 FROM ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N') '
        ELSE N''
    END;

    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF EXISTS (SELECT 1 FROM sys.identity_columns WHERE object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U''))
            SET IDENTITY_INSERT ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N' ON;
        ' + @targetHasRowsSql + N'
        INSERT INTO ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N' (' + @cols + N')
        SELECT ' + @cols + N'
        FROM ' + QUOTENAME(@SourceDb) + N'.' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N' src
        ' + CASE WHEN @pkPredicate IS NULL THEN N'' ELSE N'
        WHERE NOT EXISTS (
            SELECT 1
            FROM ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N' tgt
            WHERE ' + @pkPredicate + N'
        )' END + N';
        IF EXISTS (SELECT 1 FROM sys.identity_columns WHERE object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U''))
            SET IDENTITY_INSERT ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N' OFF;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM copy_cursor INTO @objectId, @schemaName, @tableName;
END;

CLOSE copy_cursor;
DEALLOCATE copy_cursor;

DECLARE @includeCols NVARCHAR(MAX);
DECLARE @filter NVARCHAR(MAX);
DECLARE @isUnique BIT;

DECLARE index_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT s.name, t.name, i.name, i.index_id, i.is_unique, i.filter_definition
FROM sys.indexes i
INNER JOIN sys.tables t ON t.object_id = i.object_id
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE t.object_id IN (SELECT object_id FROM @tables)
  AND i.is_primary_key = 0
  AND i.is_unique_constraint = 0
  AND i.is_hypothetical = 0
  AND i.name IS NOT NULL
  AND i.type_desc = 'NONCLUSTERED'
ORDER BY s.name, t.name, i.name;

OPEN index_cursor;
FETCH NEXT FROM index_cursor INTO @schemaName, @tableName, @constraintName, @indexId, @isUnique, @filter;

WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @objectId = OBJECT_ID(QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName));

    SELECT @cols = STRING_AGG(
        QUOTENAME(c.name) + CASE WHEN ic.is_descending_key = 1 THEN N' DESC' ELSE N' ASC' END,
        N', '
    ) WITHIN GROUP (ORDER BY ic.key_ordinal)
    FROM sys.index_columns ic
    INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE ic.object_id = @objectId
      AND ic.index_id = @indexId
      AND ic.is_included_column = 0
      AND ic.key_ordinal > 0;

    SELECT @includeCols = STRING_AGG(QUOTENAME(c.name), N', ') WITHIN GROUP (ORDER BY ic.index_column_id)
    FROM sys.index_columns ic
    INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE ic.object_id = @objectId
      AND ic.index_id = @indexId
      AND ic.is_included_column = 1;

    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'') IS NOT NULL
           AND NOT EXISTS (
                SELECT 1 FROM sys.indexes
                WHERE name = N''' + REPLACE(@constraintName, '''', '''''') + N'''
                  AND object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'')
           )
        BEGIN
            CREATE ' + CASE WHEN @isUnique = 1 THEN N'UNIQUE ' ELSE N'' END + N'NONCLUSTERED INDEX ' + QUOTENAME(@constraintName) + N'
            ON ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N' (' + @cols + N')' +
            CASE WHEN @includeCols IS NULL THEN N'' ELSE N' INCLUDE (' + @includeCols + N')' END +
            CASE WHEN @filter IS NULL THEN N'' ELSE N' WHERE ' + @filter END + N';
        END;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM index_cursor INTO @schemaName, @tableName, @constraintName, @indexId, @isUnique, @filter;
END;

CLOSE index_cursor;
DEALLOCATE index_cursor;

DECLARE @refSchema SYSNAME;
DECLARE @refTable SYSNAME;
DECLARE @fkCols NVARCHAR(MAX);
DECLARE @refCols NVARCHAR(MAX);
DECLARE @deleteAction NVARCHAR(60);
DECLARE @updateAction NVARCHAR(60);

DECLARE fk_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT
    OBJECT_SCHEMA_NAME(fk.parent_object_id) AS schema_name,
    OBJECT_NAME(fk.parent_object_id) AS table_name,
    fk.name,
    OBJECT_SCHEMA_NAME(fk.referenced_object_id) AS ref_schema,
    OBJECT_NAME(fk.referenced_object_id) AS ref_table,
    fk.delete_referential_action_desc,
    fk.update_referential_action_desc
FROM sys.foreign_keys fk
WHERE fk.parent_object_id IN (SELECT object_id FROM @tables)
  AND fk.referenced_object_id IN (SELECT object_id FROM @tables)
ORDER BY schema_name, table_name, fk.name;

OPEN fk_cursor;
FETCH NEXT FROM fk_cursor INTO @schemaName, @tableName, @constraintName, @refSchema, @refTable, @deleteAction, @updateAction;

WHILE @@FETCH_STATUS = 0
BEGIN
    SELECT @objectId = OBJECT_ID(QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName));

    SELECT @fkCols = STRING_AGG(QUOTENAME(pc.name), N', ') WITHIN GROUP (ORDER BY fkc.constraint_column_id),
           @refCols = STRING_AGG(QUOTENAME(rc.name), N', ') WITHIN GROUP (ORDER BY fkc.constraint_column_id)
    FROM sys.foreign_key_columns fkc
    INNER JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
    INNER JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
    WHERE fkc.constraint_object_id = OBJECT_ID(QUOTENAME(@schemaName) + N'.' + QUOTENAME(@constraintName));

    SET @sql = N'USE ' + QUOTENAME(@TargetDb) + N';
        IF OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'') IS NOT NULL
           AND OBJECT_ID(N''' + QUOTENAME(@refSchema) + N'.' + QUOTENAME(@refTable) + N''', N''U'') IS NOT NULL
           AND NOT EXISTS (
                SELECT 1 FROM sys.foreign_keys
                WHERE name = N''' + REPLACE(@constraintName, '''', '''''') + N'''
                  AND parent_object_id = OBJECT_ID(N''' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N''', N''U'')
           )
        BEGIN
            ALTER TABLE ' + QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName) + N'
            WITH CHECK ADD CONSTRAINT ' + QUOTENAME(@constraintName) + N'
            FOREIGN KEY (' + @fkCols + N')
            REFERENCES ' + QUOTENAME(@refSchema) + N'.' + QUOTENAME(@refTable) + N' (' + @refCols + N')' +
            CASE WHEN @deleteAction = 'NO_ACTION' THEN N'' ELSE N' ON DELETE ' + REPLACE(@deleteAction, '_', ' ') END +
            CASE WHEN @updateAction = 'NO_ACTION' THEN N'' ELSE N' ON UPDATE ' + REPLACE(@updateAction, '_', ' ') END + N';
        END;';
    EXEC sys.sp_executesql @sql;

    FETCH NEXT FROM fk_cursor INTO @schemaName, @tableName, @constraintName, @refSchema, @refTable, @deleteAction, @updateAction;
END;

CLOSE fk_cursor;
DEALLOCATE fk_cursor;

USE [$(TargetDb)];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

:r database/migrations/V007__criar_estrutura_horarios_corte.sql
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

DECLARE @SourceDb SYSNAME = N'$(SourceDb)';
DECLARE @viewName SYSNAME;
DECLARE @viewSql NVARCHAR(MAX);

DECLARE view_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT v.name
FROM [$(SourceDb)].sys.views v
INNER JOIN [$(SourceDb)].sys.schemas s ON s.schema_id = v.schema_id
WHERE s.name = N'dbo'
  AND v.name LIKE N'vw[_]%'
ORDER BY v.name;

OPEN view_cursor;
FETCH NEXT FROM view_cursor INTO @viewName;

WHILE @@FETCH_STATUS = 0
BEGIN
    IF OBJECT_ID(N'dbo.' + QUOTENAME(@viewName), N'V') IS NULL
    BEGIN
        SET @viewSql = N'CREATE VIEW dbo.' + QUOTENAME(@viewName) + N'
            AS SELECT * FROM ' + QUOTENAME(@SourceDb) + N'.dbo.' + QUOTENAME(@viewName) + N';';
        EXEC sys.sp_executesql @viewSql;
    END;

    FETCH NEXT FROM view_cursor INTO @viewName;
END;

CLOSE view_cursor;
DEALLOCATE view_cursor;
GO

:r database/migrations/V013__horarios_corte_consumir_raster_etl.sql
GO

IF OBJECT_ID(N'acesso.usuario_importacao_lotes', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.usuario_importacao_lotes (
        id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
        token_importacao    VARCHAR(80)   NOT NULL UNIQUE,
        arquivo_nome        NVARCHAR(255) NOT NULL,
        payload_json        NVARCHAR(MAX) NOT NULL,
        criado_por          VARCHAR(80)   NULL,
        criado_em           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
        expira_em           DATETIME2     NOT NULL
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_usuario_importacao_lotes_expira_em'
      AND object_id = OBJECT_ID(N'acesso.usuario_importacao_lotes', N'U')
)
BEGIN
    CREATE INDEX IX_usuario_importacao_lotes_expira_em
        ON acesso.usuario_importacao_lotes(expira_em);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_usuarios_chave_legado_not_null'
      AND object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_usuarios_chave_legado_not_null
        ON acesso.usuarios (chave_legado)
        WHERE chave_legado IS NOT NULL;
END;
GO

:r database/migrations/V010__garantir_usuario_supremo_desenvolvedor.sql
GO

:r database/migrations/V011__adicionar_escopo_filiais_usuario.sql
GO

:r database/migrations/V049__criar_viagem_justificativas.sql
GO

:r database/migrations/V050__adicionar_soft_delete_justificativas.sql
GO

:r database/migrations/V051__normalizar_chaves_metas_manifestos.sql
GO

:r database/migrations/V052__criar_excecao_cubagem_clientes.sql
GO

:r database/migrations/V053__adicionar_soft_delete_excecao_cubagem_clientes.sql
GO

:r database/migrations/V054__adicionar_heartbeat_sessao_usuarios.sql
GO

IF SUSER_ID(N'$(AppLogin)') IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'$(AppLogin)')
    BEGIN
        CREATE USER [$(AppLogin)] FOR LOGIN [$(AppLogin)];
    END;

    IF IS_ROLEMEMBER(N'db_datareader', N'$(AppLogin)') <> 1
        ALTER ROLE [db_datareader] ADD MEMBER [$(AppLogin)];

    IF IS_ROLEMEMBER(N'db_datawriter', N'$(AppLogin)') <> 1
        ALTER ROLE [db_datawriter] ADD MEMBER [$(AppLogin)];
END;
GO

SELECT
    DB_NAME() AS database_name,
    s.name AS schema_name,
    t.name AS table_name,
    SUM(p.rows) AS total_rows
FROM sys.tables t
INNER JOIN sys.schemas s ON s.schema_id = t.schema_id
INNER JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
WHERE s.name = 'acesso'
GROUP BY s.name, t.name
ORDER BY s.name, t.name;
GO

