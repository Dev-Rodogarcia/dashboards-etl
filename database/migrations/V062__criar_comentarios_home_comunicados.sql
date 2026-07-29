SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

CREATE TABLE acesso.home_comunicado_comentarios (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    comunicado_id   BIGINT NOT NULL REFERENCES acesso.home_comunicados(id),
    usuario_id      BIGINT NOT NULL REFERENCES acesso.usuarios(id),
    corpo           NVARCHAR(700) NOT NULL,
    ativo           BIT NOT NULL CONSTRAINT DF_home_comunicado_comentarios_ativo DEFAULT 1,
    criado_em       DATETIME2(0) NOT NULL CONSTRAINT DF_home_comunicado_comentarios_criado_em DEFAULT SYSUTCDATETIME(),
    atualizado_em   DATETIME2(0) NOT NULL CONSTRAINT DF_home_comunicado_comentarios_atualizado_em DEFAULT SYSUTCDATETIME()
);
GO

CREATE INDEX IX_home_comunicado_comentarios_comunicado_ativo_criado
    ON acesso.home_comunicado_comentarios (comunicado_id, ativo, criado_em, id);
GO
