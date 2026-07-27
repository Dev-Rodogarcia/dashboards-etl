SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF COL_LENGTH(N'acesso.home_solicitacoes_melhoria', N'local_aplicacao') IS NULL
BEGIN
    ALTER TABLE acesso.home_solicitacoes_melhoria
    ADD local_aplicacao NVARCHAR(500) NULL;
END;
GO
