SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF SCHEMA_ID(N'acesso') IS NULL
BEGIN
    THROW 52300, 'Schema acesso ausente. Aplique as migrations anteriores antes da V023.', 1;
END;
GO

IF OBJECT_ID(N'acesso.configuracoes_seguranca', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.configuracoes_seguranca (
        chave         VARCHAR(100)  NOT NULL PRIMARY KEY,
        valor         NVARCHAR(500) NOT NULL,
        atualizado_em DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF COL_LENGTH(N'acesso.configuracoes_seguranca', N'chave') IS NULL
   OR COL_LENGTH(N'acesso.configuracoes_seguranca', N'valor') IS NULL
   OR COL_LENGTH(N'acesso.configuracoes_seguranca', N'atualizado_em') IS NULL
BEGIN
    THROW 52301, 'Tabela acesso.configuracoes_seguranca existe com contrato invalido.', 1;
END;
GO

IF OBJECT_ID(N'acesso.usuarios', N'U') IS NULL
BEGIN
    THROW 52302, 'Tabela acesso.usuarios ausente. Aplique as migrations anteriores antes da V023.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM acesso.usuarios
    WHERE chave_legado IS NOT NULL
    GROUP BY chave_legado
    HAVING COUNT(1) > 1
)
BEGIN
    THROW 52303, 'Duplicidade em acesso.usuarios.chave_legado impede recriacao do indice filtrado.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UX_usuarios_chave_legado_not_null'
      AND parent_object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
BEGIN
    ALTER TABLE acesso.usuarios
    DROP CONSTRAINT UX_usuarios_chave_legado_not_null;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_usuarios_chave_legado_not_null'
      AND object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
AND NOT EXISTS (
    SELECT 1
    FROM sys.indexes i
    WHERE i.name = N'UX_usuarios_chave_legado_not_null'
      AND i.object_id = OBJECT_ID(N'acesso.usuarios', N'U')
      AND i.is_unique = 1
      AND REPLACE(REPLACE(REPLACE(UPPER(ISNULL(i.filter_definition, N'')), N'[', N''), N']', N''), N' ', N'') = N'(CHAVE_LEGADOISNOTNULL)'
      AND (
          SELECT COUNT(1)
          FROM sys.index_columns ic
          WHERE ic.object_id = i.object_id
            AND ic.index_id = i.index_id
            AND ic.key_ordinal > 0
            AND ic.is_included_column = 0
      ) = 1
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns ic
          INNER JOIN sys.columns c
              ON c.object_id = ic.object_id
             AND c.column_id = ic.column_id
          WHERE ic.object_id = i.object_id
            AND ic.index_id = i.index_id
            AND ic.key_ordinal = 1
            AND ic.is_included_column = 0
            AND c.name = N'chave_legado'
      )
)
BEGIN
    DROP INDEX UX_usuarios_chave_legado_not_null ON acesso.usuarios;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_usuarios_chave_legado_not_null'
      AND object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_usuarios_chave_legado_not_null
    ON acesso.usuarios (chave_legado)
    WHERE chave_legado IS NOT NULL;
END;
GO

IF OBJECT_ID(N'acesso.fretes_goals', N'U') IS NULL
BEGIN
    THROW 52304, 'Tabela acesso.fretes_goals ausente. Aplique a migration V017 antes da V023.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM acesso.fretes_goals
    WHERE branch_id IS NOT NULL
    GROUP BY branch_id, ano, mes
    HAVING COUNT(1) > 1
)
BEGIN
    THROW 52305, 'Duplicidade de metas por filial/periodo impede recriacao de UX_fretes_goals_branch_period.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM acesso.fretes_goals
    WHERE branch_id IS NULL
    GROUP BY ano, mes
    HAVING COUNT(1) > 1
)
BEGIN
    THROW 52306, 'Duplicidade de metas globais por periodo impede recriacao de UX_fretes_goals_global_period.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UX_fretes_goals_branch_period'
      AND parent_object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
BEGIN
    ALTER TABLE acesso.fretes_goals
    DROP CONSTRAINT UX_fretes_goals_branch_period;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_fretes_goals_branch_period'
      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
AND NOT EXISTS (
    SELECT 1
    FROM sys.indexes i
    WHERE i.name = N'UX_fretes_goals_branch_period'
      AND i.object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
      AND i.is_unique = 1
      AND REPLACE(REPLACE(REPLACE(UPPER(ISNULL(i.filter_definition, N'')), N'[', N''), N']', N''), N' ', N'') = N'(BRANCH_IDISNOTNULL)'
      AND (
          SELECT COUNT(1)
          FROM sys.index_columns ic
          WHERE ic.object_id = i.object_id
            AND ic.index_id = i.index_id
            AND ic.key_ordinal > 0
            AND ic.is_included_column = 0
      ) = 3
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns ic
          INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
          WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
            AND ic.key_ordinal = 1 AND ic.is_included_column = 0 AND c.name = N'branch_id'
      )
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns ic
          INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
          WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
            AND ic.key_ordinal = 2 AND ic.is_included_column = 0 AND c.name = N'ano'
      )
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns ic
          INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
          WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
            AND ic.key_ordinal = 3 AND ic.is_included_column = 0 AND c.name = N'mes'
      )
)
BEGIN
    DROP INDEX UX_fretes_goals_branch_period ON acesso.fretes_goals;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_fretes_goals_branch_period'
      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_fretes_goals_branch_period
    ON acesso.fretes_goals (branch_id, ano, mes)
    WHERE branch_id IS NOT NULL;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'UX_fretes_goals_global_period'
      AND parent_object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
BEGIN
    ALTER TABLE acesso.fretes_goals
    DROP CONSTRAINT UX_fretes_goals_global_period;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_fretes_goals_global_period'
      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
AND NOT EXISTS (
    SELECT 1
    FROM sys.indexes i
    WHERE i.name = N'UX_fretes_goals_global_period'
      AND i.object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
      AND i.is_unique = 1
      AND REPLACE(REPLACE(REPLACE(UPPER(ISNULL(i.filter_definition, N'')), N'[', N''), N']', N''), N' ', N'') = N'(BRANCH_IDISNULL)'
      AND (
          SELECT COUNT(1)
          FROM sys.index_columns ic
          WHERE ic.object_id = i.object_id
            AND ic.index_id = i.index_id
            AND ic.key_ordinal > 0
            AND ic.is_included_column = 0
      ) = 2
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns ic
          INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
          WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
            AND ic.key_ordinal = 1 AND ic.is_included_column = 0 AND c.name = N'ano'
      )
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns ic
          INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
          WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
            AND ic.key_ordinal = 2 AND ic.is_included_column = 0 AND c.name = N'mes'
      )
)
BEGIN
    DROP INDEX UX_fretes_goals_global_period ON acesso.fretes_goals;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_fretes_goals_global_period'
      AND object_id = OBJECT_ID(N'acesso.fretes_goals', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_fretes_goals_global_period
    ON acesso.fretes_goals (ano, mes)
    WHERE branch_id IS NULL;
END;
GO
