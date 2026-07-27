IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_home_solicitacoes_melhoria_solicitante_ativo_status_criado'
      AND object_id = OBJECT_ID(N'acesso.home_solicitacoes_melhoria', N'U')
)
BEGIN
    CREATE INDEX IX_home_solicitacoes_melhoria_solicitante_ativo_status_criado
    ON acesso.home_solicitacoes_melhoria (solicitante_email, ativo, status, criado_em DESC, id DESC);
END;
GO
