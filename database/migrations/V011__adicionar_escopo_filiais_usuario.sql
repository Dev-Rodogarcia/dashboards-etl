SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF COL_LENGTH(N'acesso.usuarios', N'escopo_filiais_tipo') IS NULL
BEGIN
    ALTER TABLE acesso.usuarios
    ADD escopo_filiais_tipo VARCHAR(20) NOT NULL
        CONSTRAINT DF_usuarios_escopo_filiais_tipo DEFAULT 'HERDAR_SETOR';
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_usuarios_escopo_filiais_tipo'
      AND parent_object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
BEGIN
    ALTER TABLE acesso.usuarios
    WITH CHECK ADD CONSTRAINT CK_usuarios_escopo_filiais_tipo
    CHECK (escopo_filiais_tipo IN ('HERDAR_SETOR', 'TODAS', 'SELECIONADAS'));
END;
GO

IF OBJECT_ID(N'acesso.usuario_filiais_permitidas', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.usuario_filiais_permitidas (
        usuario_id  BIGINT        NOT NULL REFERENCES acesso.usuarios(id),
        filial_nome NVARCHAR(120) NOT NULL,
        PRIMARY KEY (usuario_id, filial_nome)
    );
END;
GO
