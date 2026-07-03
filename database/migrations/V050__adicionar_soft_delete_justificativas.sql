IF COL_LENGTH(N'dbo.viagem_justificativas', N'ativo') IS NULL
BEGIN
    ALTER TABLE dbo.viagem_justificativas
        ADD ativo BIT NOT NULL
            CONSTRAINT DF_viagem_justificativas_ativo DEFAULT 1 WITH VALUES;
END;
