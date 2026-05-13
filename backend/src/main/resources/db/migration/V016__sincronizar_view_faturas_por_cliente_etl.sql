IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 51600, 'Database ETL_SISTEMA nao encontrada para sincronizar a view de faturas por cliente.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_faturas_por_cliente_powerbi', N'V') IS NULL
BEGIN
    THROW 51601, 'View ETL_SISTEMA.dbo.vw_faturas_por_cliente_powerbi nao encontrada. Aplique as views da ETL antes desta migration.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'ID Único'),
        (N'Cliente/CNPJ'),
        (N'Status do Processo'),
        (N'Fatura/N° Documento'),
        (N'Fatura/Emissão'),
        (N'Fatura/Valor'),
        (N'CT-e/Data de emissão')
    ) AS obrigatorias(nome)
    WHERE NOT EXISTS (
        SELECT 1
        FROM ETL_SISTEMA.INFORMATION_SCHEMA.COLUMNS c
        WHERE c.TABLE_SCHEMA = N'dbo'
          AND c.TABLE_NAME = N'vw_faturas_por_cliente_powerbi'
          AND c.COLUMN_NAME = obrigatorias.nome
    )
)
BEGIN
    THROW 51602, 'View de faturas por cliente da ETL sem colunas exigidas pelo dashboard.', 1;
END;

EXEC(N'
CREATE OR ALTER VIEW dbo.vw_faturas_por_cliente_powerbi
AS
SELECT
    [Hora (Solicitacao)],
    [ID Único],
    [Filial],
    [Estado],
    [CT-e/Número],
    [Número do Documento],
    [CT-e/Chave],
    [CT-e/Data de emissão],
    [Frete/Valor dos CT-es],
    [Terceiros/Valor CT-es],
    [CT-e/Status],
    [CT-e/Resultado],
    [Tipo],
    [Classificação],
    [Pagador do frete/Nome],
    [Pagador do frete/Documento],
    [Cliente/CNPJ],
    [Remetente/Nome],
    [Remetente/Documento],
    [Destinatário/Nome],
    [Destinatário/Documento],
    [Vendedor/Nome],
    [NFS-e/Número],
    [NFS-e/Série],
    [fit_nse_number],
    [N° NFS-e],
    [Carteira/Descrição],
    [Instrução Customizada],
    [Status do Processo],
    [Fatura/N° Documento],
    [Fatura/Emissão],
    [Fatura/Valor],
    [Fatura/Valor Total],
    [Fatura/Número],
    [Fatura/Emissão Fatura],
    [Parcelas/Vencimento],
    [Fatura/Baixa],
    [Fatura/Data Vencimento Original],
    [Notas Fiscais],
    [Pedidos/Cliente],
    [Metadata],
    [Data da Última Atualização]
FROM [ETL_SISTEMA].dbo.vw_faturas_por_cliente_powerbi;
');
