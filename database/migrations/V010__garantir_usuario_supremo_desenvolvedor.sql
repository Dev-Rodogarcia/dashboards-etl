SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
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

CREATE OR ALTER TRIGGER acesso.TR_usuarios_proteger_usuario_supremo
ON acesso.usuarios
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @usuarioSupremoId BIGINT;

    SELECT @usuarioSupremoId = TRY_CONVERT(BIGINT, valor)
    FROM acesso.configuracoes_seguranca
    WHERE chave = 'usuario_supremo_id';

    IF @usuarioSupremoId IS NULL
    BEGIN
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM deleted d
        LEFT JOIN inserted i ON i.id = d.id
        WHERE d.id = @usuarioSupremoId
          AND (
              i.id IS NULL
              OR i.email <> d.email
              OR i.login <> d.login
              OR i.ativo = 0
          )
    )
    BEGIN
        THROW 51010, 'Usuário supremo não pode ser excluído, renomeado ou inativado.', 1;
    END;
END;
GO

CREATE OR ALTER TRIGGER acesso.TR_usuario_papel_proteger_usuario_supremo
ON acesso.usuario_papel_vinculos
AFTER DELETE, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @usuarioSupremoId BIGINT;
    DECLARE @papelSupremoId BIGINT;

    SELECT @usuarioSupremoId = TRY_CONVERT(BIGINT, valor)
    FROM acesso.configuracoes_seguranca
    WHERE chave = 'usuario_supremo_id';

    SELECT @papelSupremoId = TRY_CONVERT(BIGINT, valor)
    FROM acesso.configuracoes_seguranca
    WHERE chave = 'papel_supremo_id';

    IF @usuarioSupremoId IS NULL OR @papelSupremoId IS NULL
    BEGIN
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM deleted d
        WHERE d.usuario_id = @usuarioSupremoId
    )
    AND NOT EXISTS (
        SELECT 1
        FROM acesso.usuario_papel_vinculos v
        WHERE v.usuario_id = @usuarioSupremoId
          AND v.papel_id = @papelSupremoId
    )
    BEGIN
        THROW 51011, 'Usuário supremo não pode perder o papel configurado como supremo.', 1;
    END;
END;
GO
