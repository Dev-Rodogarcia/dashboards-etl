SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

DECLARE @constraintName SYSNAME;

SELECT TOP 1 @constraintName = kc.name
FROM sys.key_constraints kc
INNER JOIN sys.index_columns ic
    ON ic.object_id = kc.parent_object_id
   AND ic.index_id = kc.unique_index_id
INNER JOIN sys.columns c
    ON c.object_id = ic.object_id
   AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID(N'acesso.usuarios', N'U')
  AND kc.[type] = 'UQ'
  AND c.name = 'chave_legado'
  AND NOT EXISTS (
      SELECT 1
      FROM sys.index_columns ic2
      INNER JOIN sys.columns c2
          ON c2.object_id = ic2.object_id
         AND c2.column_id = ic2.column_id
      WHERE ic2.object_id = kc.parent_object_id
        AND ic2.index_id = kc.unique_index_id
        AND c2.name <> 'chave_legado'
  );

IF @constraintName IS NOT NULL
BEGIN
    DECLARE @dropConstraintSql NVARCHAR(MAX) =
        N'ALTER TABLE acesso.usuarios DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @dropConstraintSql;
END;
GO

DECLARE @indexName SYSNAME;

SELECT TOP 1 @indexName = i.name
FROM sys.indexes i
INNER JOIN sys.index_columns ic
    ON ic.object_id = i.object_id
   AND ic.index_id = i.index_id
INNER JOIN sys.columns c
    ON c.object_id = ic.object_id
   AND c.column_id = ic.column_id
WHERE i.object_id = OBJECT_ID(N'acesso.usuarios', N'U')
  AND i.is_unique = 1
  AND i.name <> 'UX_usuarios_chave_legado_not_null'
  AND c.name = 'chave_legado'
  AND NOT EXISTS (
      SELECT 1
      FROM sys.index_columns ic2
      INNER JOIN sys.columns c2
          ON c2.object_id = ic2.object_id
         AND c2.column_id = ic2.column_id
      WHERE ic2.object_id = i.object_id
        AND ic2.index_id = i.index_id
        AND c2.name <> 'chave_legado'
  );

IF @indexName IS NOT NULL
BEGIN
    DECLARE @dropIndexSql NVARCHAR(MAX) =
        N'DROP INDEX ' + QUOTENAME(@indexName) + N' ON acesso.usuarios';
    EXEC sp_executesql @dropIndexSql;
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
