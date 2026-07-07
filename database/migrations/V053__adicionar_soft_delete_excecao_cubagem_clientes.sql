IF COL_LENGTH(N'dbo.cliente_excecao_cubagem', N'ativo') IS NULL
BEGIN
    ALTER TABLE dbo.cliente_excecao_cubagem
        ADD ativo BIT NOT NULL
            CONSTRAINT DF_cliente_excecao_cubagem_ativo DEFAULT 1 WITH VALUES;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_cliente_excecao_cubagem_ativo_razao_social'
      AND object_id = OBJECT_ID(N'dbo.cliente_excecao_cubagem', N'U')
)
BEGIN
    CREATE INDEX IX_cliente_excecao_cubagem_ativo_razao_social
        ON dbo.cliente_excecao_cubagem (ativo, razao_social, cliente_cnpj);
END;
