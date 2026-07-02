IF OBJECT_ID(N'dbo.viagem_justificativas', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.viagem_justificativas (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_viagem_justificativas PRIMARY KEY,
        cod_solicitacao BIGINT NOT NULL,
        justificativa NVARCHAR(1000) NOT NULL,
        criado_em DATETIMEOFFSET NOT NULL CONSTRAINT DF_viagem_justificativas_criado_em DEFAULT GETUTCDATE(),
        criado_por NVARCHAR(255) NOT NULL
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_viagem_justificativas_cod_solicitacao'
      AND object_id = OBJECT_ID(N'dbo.viagem_justificativas', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_viagem_justificativas_cod_solicitacao
        ON dbo.viagem_justificativas(cod_solicitacao);
END;
