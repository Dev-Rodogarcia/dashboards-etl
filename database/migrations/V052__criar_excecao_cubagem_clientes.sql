IF OBJECT_ID(N'dbo.cliente_excecao_cubagem', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cliente_excecao_cubagem (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cliente_excecao_cubagem PRIMARY KEY,
        cliente_cnpj VARCHAR(14) NOT NULL,
        razao_social NVARCHAR(255) NULL,
        nome_fantasia NVARCHAR(255) NULL,
        cidade_uf NVARCHAR(150) NULL,
        atualizado_por NVARCHAR(100) NULL,
        data_atualizacao DATETIMEOFFSET NOT NULL CONSTRAINT DF_cliente_excecao_cubagem_data_atualizacao DEFAULT SYSDATETIMEOFFSET()
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_cliente_excecao_cubagem_cnpj_numerico'
      AND parent_object_id = OBJECT_ID(N'dbo.cliente_excecao_cubagem', N'U')
)
BEGIN
    ALTER TABLE dbo.cliente_excecao_cubagem WITH CHECK
    ADD CONSTRAINT CK_cliente_excecao_cubagem_cnpj_numerico
        CHECK (cliente_cnpj NOT LIKE '%[^0-9]%' AND LEN(cliente_cnpj) = 14);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_cliente_excecao_cubagem_cliente_cnpj'
      AND object_id = OBJECT_ID(N'dbo.cliente_excecao_cubagem', N'U')
)
BEGIN
    CREATE UNIQUE INDEX UX_cliente_excecao_cubagem_cliente_cnpj
        ON dbo.cliente_excecao_cubagem (cliente_cnpj);
END;

DECLARE @clientes_legados TABLE (
    cliente_cnpj VARCHAR(14) NOT NULL PRIMARY KEY
);

INSERT INTO @clientes_legados (cliente_cnpj)
VALUES
    ('44699346000103'),
    ('07668944000180'),
    ('13190609000546'),
    ('13190609000384'),
    ('13190609000627'),
    ('46928552000165'),
    ('14675270007381'),
    ('56643018010390'),
    ('14675270000450'),
    ('14675270000298'),
    ('05396883001510'),
    ('05396883000386'),
    ('51602373000173'),
    ('43829282000651'),
    ('43829282000147'),
    ('43829282000490'),
    ('03944724000696'),
    ('03944724000777'),
    ('03944724000262'),
    ('03944724000939'),
    ('03944724000858'),
    ('44381747000102'),
    ('01459630000272'),
    ('43996693003061'),
    ('43996693000631'),
    ('43996693000208'),
    ('43996693002766'),
    ('43996693002928'),
    ('43996693002847'),
    ('43996693000801'),
    ('43996693000127'),
    ('92599901000160'),
    ('33064262000250'),
    ('08862530000827'),
    ('08862530000231'),
    ('08862530000150'),
    ('33064262000179'),
    ('08862530000746'),
    ('08862530001122'),
    ('08862530001203');

MERGE dbo.cliente_excecao_cubagem AS target
USING @clientes_legados AS source
   ON target.cliente_cnpj = source.cliente_cnpj
WHEN NOT MATCHED THEN
    INSERT (cliente_cnpj, atualizado_por)
    VALUES (source.cliente_cnpj, N'migration:V052');
