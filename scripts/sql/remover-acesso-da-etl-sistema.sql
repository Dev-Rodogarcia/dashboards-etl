:setvar SourceDb ETL_SISTEMA
:setvar TargetDb DASHBOARDS

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE [$(SourceDb)];
GO

IF DB_ID(N'$(SourceDb)') IS NULL
BEGIN
    THROW 51100, 'Database de origem não encontrada.', 1;
END;

IF DB_ID(N'$(TargetDb)') IS NULL
BEGIN
    THROW 51101, 'Database DASHBOARDS não encontrada. Não é seguro remover acesso da ETL_SISTEMA.', 1;
END;

IF SCHEMA_ID(N'acesso') IS NULL
BEGIN
    PRINT 'Schema acesso não existe na ETL_SISTEMA. Nada a remover.';
    RETURN;
END;

IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    WHERE (OBJECT_SCHEMA_NAME(fk.parent_object_id) = N'acesso'
        OR OBJECT_SCHEMA_NAME(fk.referenced_object_id) = N'acesso')
      AND NOT (
          OBJECT_SCHEMA_NAME(fk.parent_object_id) = N'acesso'
          AND OBJECT_SCHEMA_NAME(fk.referenced_object_id) = N'acesso'
      )
)
BEGIN
    THROW 51102, 'Existem FKs externas apontando para/ou saindo de ETL_SISTEMA.acesso. Remoção abortada.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM sys.sql_expression_dependencies
    WHERE referenced_schema_name = N'acesso'
      AND OBJECT_SCHEMA_NAME(referencing_id) <> N'acesso'
)
BEGIN
    THROW 51103, 'Existem dependências SQL externas ao schema acesso. Remoção abortada.', 1;
END;

DECLARE @faltantes TABLE (table_name SYSNAME NOT NULL);

INSERT INTO @faltantes (table_name)
SELECT src.name
FROM sys.tables src
INNER JOIN sys.schemas src_schema ON src_schema.schema_id = src.schema_id
WHERE src_schema.name = N'acesso'
  AND NOT EXISTS (
      SELECT 1
      FROM [$(TargetDb)].sys.tables tgt
      INNER JOIN [$(TargetDb)].sys.schemas tgt_schema ON tgt_schema.schema_id = tgt.schema_id
      WHERE tgt_schema.name = N'acesso'
        AND tgt.name = src.name
  );

IF EXISTS (SELECT 1 FROM @faltantes)
BEGIN
    DECLARE @faltantesTexto NVARCHAR(MAX) = (
        SELECT STRING_AGG(table_name, N', ') FROM @faltantes
    );
    THROW 51104, @faltantesTexto, 1;
END;

DECLARE @comparacao TABLE (
    table_name SYSNAME NOT NULL,
    source_rows BIGINT NOT NULL,
    target_rows BIGINT NOT NULL
);

INSERT INTO @comparacao (table_name, source_rows, target_rows)
SELECT
    src.name,
    src_counts.total_rows,
    tgt_counts.total_rows
FROM sys.tables src
INNER JOIN sys.schemas src_schema ON src_schema.schema_id = src.schema_id
CROSS APPLY (
    SELECT SUM(p.rows) AS total_rows
    FROM sys.partitions p
    WHERE p.object_id = src.object_id
      AND p.index_id IN (0, 1)
) src_counts
INNER JOIN [$(TargetDb)].sys.tables tgt
    ON tgt.name = src.name
INNER JOIN [$(TargetDb)].sys.schemas tgt_schema
    ON tgt_schema.schema_id = tgt.schema_id
   AND tgt_schema.name = N'acesso'
CROSS APPLY (
    SELECT SUM(p.rows) AS total_rows
    FROM [$(TargetDb)].sys.partitions p
    WHERE p.object_id = tgt.object_id
      AND p.index_id IN (0, 1)
) tgt_counts
WHERE src_schema.name = N'acesso';

IF EXISTS (
    SELECT 1
    FROM @comparacao
    WHERE target_rows < source_rows
)
BEGIN
    SELECT table_name, source_rows, target_rows
    FROM @comparacao
    WHERE target_rows < source_rows
    ORDER BY table_name;

    THROW 51105, 'DASHBOARDS tem menos linhas que ETL_SISTEMA em uma ou mais tabelas de acesso. Remoção abortada.', 1;
END;

BEGIN TRANSACTION;

DECLARE @sql NVARCHAR(MAX) = N'';

SELECT @sql = STRING_AGG(
    N'ALTER TABLE ' + QUOTENAME(SCHEMA_NAME(parent.schema_id)) + N'.' + QUOTENAME(parent.name)
    + N' DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';',
    CHAR(10)
)
FROM sys.foreign_keys fk
INNER JOIN sys.tables parent ON parent.object_id = fk.parent_object_id
INNER JOIN sys.tables referenced ON referenced.object_id = fk.referenced_object_id
WHERE SCHEMA_NAME(parent.schema_id) = N'acesso'
  AND SCHEMA_NAME(referenced.schema_id) = N'acesso';

IF @sql IS NOT NULL AND LEN(@sql) > 0
BEGIN
    EXEC sys.sp_executesql @sql;
END;

DROP TABLE IF EXISTS acesso.usuario_importacao_lotes;
DROP TABLE IF EXISTS acesso.refresh_tokens;
DROP TABLE IF EXISTS acesso.audit_logs;
DROP TABLE IF EXISTS acesso.usuario_permissao_overrides;
DROP TABLE IF EXISTS acesso.usuario_papel_vinculos;
DROP TABLE IF EXISTS acesso.setor_filiais_permitidas;
DROP TABLE IF EXISTS acesso.setor_permissao_templates;
DROP TABLE IF EXISTS acesso.usuarios;
DROP TABLE IF EXISTS acesso.papeis;
DROP TABLE IF EXISTS acesso.permissoes;
DROP TABLE IF EXISTS acesso.setores;

IF EXISTS (
    SELECT 1
    FROM sys.objects
    WHERE schema_id = SCHEMA_ID(N'acesso')
)
BEGIN
    THROW 51106, 'Schema acesso ainda contém objetos após a remoção das tabelas. Transação abortada.', 1;
END;

DROP SCHEMA acesso;

COMMIT TRANSACTION;

SELECT
    DB_NAME() AS database_name,
    CASE WHEN SCHEMA_ID(N'acesso') IS NULL THEN 'REMOVIDO' ELSE 'AINDA_EXISTE' END AS acesso_status;
GO
