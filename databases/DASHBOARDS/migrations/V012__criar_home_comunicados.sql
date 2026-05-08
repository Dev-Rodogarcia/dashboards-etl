SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'acesso.home_comunicados', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.home_comunicados (
        id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        titulo         NVARCHAR(140)        NOT NULL,
        corpo          NVARCHAR(700)        NOT NULL,
        tag            VARCHAR(20)          NOT NULL,
        publico_alvo   NVARCHAR(140)        NOT NULL CONSTRAINT DF_home_comunicados_publico_alvo DEFAULT N'Todos',
        publicado_em   DATETIME2(0)         NOT NULL CONSTRAINT DF_home_comunicados_publicado_em DEFAULT SYSUTCDATETIME(),
        ativo          BIT                  NOT NULL CONSTRAINT DF_home_comunicados_ativo DEFAULT 1,
        criado_por     NVARCHAR(120)        NULL,
        criado_em      DATETIME2(0)         NOT NULL CONSTRAINT DF_home_comunicados_criado_em DEFAULT SYSUTCDATETIME(),
        atualizado_por NVARCHAR(120)        NULL,
        atualizado_em  DATETIME2(0)         NULL
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_home_comunicados_tag'
      AND parent_object_id = OBJECT_ID(N'acesso.home_comunicados', N'U')
)
BEGIN
    ALTER TABLE acesso.home_comunicados
    WITH CHECK ADD CONSTRAINT CK_home_comunicados_tag
    CHECK (tag IN ('NOVO', 'ATENCAO', 'FIXADO'));
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_home_comunicados_ativo_publicado'
      AND object_id = OBJECT_ID(N'acesso.home_comunicados', N'U')
)
BEGIN
    CREATE INDEX IX_home_comunicados_ativo_publicado
    ON acesso.home_comunicados (ativo, publicado_em DESC, id DESC);
END;
GO

IF NOT EXISTS (SELECT 1 FROM acesso.home_comunicados)
BEGIN
    INSERT INTO acesso.home_comunicados (titulo, corpo, tag, publico_alvo, criado_por)
    VALUES
        (N'Indicadores de Gestão à Vista disponíveis', N'Performance de entrega, coletores, cubagem, indenização e horários de corte centralizados no painel operacional.', 'NOVO', N'Operação, TI e Diretoria', N'sistema'),
        (N'Acesso por setor segue permissões efetivas', N'A Home mostra somente atalhos liberados para o usuário autenticado, respeitando setor, papel e exceções individuais.', 'FIXADO', N'Todos', N'sistema'),
        (N'Monitoramento do ETL em destaque', N'Acompanhe execuções, volume processado e erros no painel ETL Saúde quando a permissão estiver liberada.', 'ATENCAO', N'TI e administradores', N'sistema');
END;
GO
