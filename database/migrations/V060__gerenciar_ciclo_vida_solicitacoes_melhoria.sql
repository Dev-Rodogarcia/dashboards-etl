SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF COL_LENGTH(N'acesso.home_solicitacoes_melhoria', N'arquivado_em') IS NULL
BEGIN
    ALTER TABLE acesso.home_solicitacoes_melhoria ADD arquivado_em DATETIME2(0) NULL;
END;
GO

IF COL_LENGTH(N'acesso.home_solicitacoes_melhoria', N'excluido_em') IS NULL
BEGIN
    ALTER TABLE acesso.home_solicitacoes_melhoria ADD excluido_em DATETIME2(0) NULL;
END;
GO

DECLARE @constraintName SYSNAME;
DECLARE @sql NVARCHAR(MAX);
SELECT @constraintName = kc.name
FROM sys.check_constraints kc
WHERE kc.parent_object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U')
  AND kc.name = N'CK_home_solicitacoes_melhoria_status';

IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE acesso.home_solicitacoes_melhoria DROP CONSTRAINT ' + QUOTENAME(@constraintName) + N';';
    EXEC sys.sp_executesql @sql;
END;
GO

ALTER TABLE acesso.home_solicitacoes_melhoria
WITH CHECK ADD CONSTRAINT CK_home_solicitacoes_melhoria_status
CHECK (status IN ('ABERTA', 'CONCLUIDA', 'ARQUIVADA'));
GO

UPDATE acesso.home_solicitacoes_melhoria
SET status = 'ARQUIVADA',
    ativo = 1,
    arquivado_em = COALESCE(atualizado_em, criado_em)
WHERE ativo = 0
  AND excluido_em IS NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_home_solicitacoes_melhoria_retencao'
      AND object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U')
)
BEGIN
    CREATE INDEX IX_home_solicitacoes_melhoria_retencao
        ON acesso.home_solicitacoes_melhoria (ativo, status, concluido_em, arquivado_em, id);
END;
GO
