IF DB_ID(N'ETL_SISTEMA') IS NULL
BEGIN
    THROW 52200, 'Database ETL_SISTEMA nao encontrada para sincronizar vw_manifestos_powerbi.', 1;
END;

IF OBJECT_ID(N'ETL_SISTEMA.dbo.vw_manifestos_powerbi', N'V') IS NULL
BEGIN
    THROW 52201, 'View ETL_SISTEMA.dbo.vw_manifestos_powerbi ausente. Aplique as migrations do ETL antes desta migration.', 1;
END;

IF NOT EXISTS (
    SELECT 1
    FROM ETL_SISTEMA.sys.columns c
    JOIN ETL_SISTEMA.sys.objects o ON o.object_id = c.object_id
    JOIN ETL_SISTEMA.sys.schemas s ON s.schema_id = o.schema_id
    WHERE s.name = N'dbo'
      AND o.name = N'vw_manifestos_powerbi'
      AND c.name IN (N'Tipo Motorista', N'Proprietário/Documento')
    GROUP BY o.object_id
    HAVING COUNT(DISTINCT c.name) = 2
)
BEGIN
    THROW 52202, 'Contrato invalido: ETL_SISTEMA.dbo.vw_manifestos_powerbi deve publicar [Tipo Motorista] e [Proprietário/Documento].', 1;
END;

CREATE OR ALTER VIEW dbo.vw_manifestos_powerbi
AS
SELECT *
FROM [ETL_SISTEMA].dbo.vw_manifestos_powerbi;

EXEC sys.sp_refreshview N'dbo.vw_manifestos_powerbi';
