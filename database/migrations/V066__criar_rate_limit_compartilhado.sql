IF OBJECT_ID(N'acesso.rate_limit_buckets', N'U') IS NULL
BEGIN
    CREATE TABLE acesso.rate_limit_buckets (
        chave             VARCHAR(300) NOT NULL CONSTRAINT PK_rate_limit_buckets PRIMARY KEY,
        total_na_janela   INT NOT NULL CONSTRAINT CK_rate_limit_buckets_total_positivo CHECK (total_na_janela >= 0),
        expira_em         DATETIME2(3) NOT NULL,
        atualizado_em     DATETIME2(3) NOT NULL CONSTRAINT DF_rate_limit_buckets_atualizado DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'acesso.rate_limit_buckets', N'U')
      AND name = N'IX_rate_limit_buckets_expira_em'
)
BEGIN
    CREATE INDEX IX_rate_limit_buckets_expira_em
        ON acesso.rate_limit_buckets (expira_em);
END;
