IF DB_ID(N'ETL_SISTEMA') IS NULL
    THROW 51290, 'Database fonte ETL_SISTEMA nao encontrado para publicacao de dbo.vw_fato_manifestos_dash.', 1;
GO

IF OBJECT_ID(N'dbo.vw_fato_manifestos_dash', N'V') IS NOT NULL DROP VIEW dbo.vw_fato_manifestos_dash;
IF EXISTS (
    SELECT 1
    FROM sys.synonyms
    WHERE schema_id = SCHEMA_ID(N'dbo')
      AND name = N'vw_fato_manifestos_dash'
)
    DROP SYNONYM dbo.vw_fato_manifestos_dash;
GO

CREATE SYNONYM dbo.vw_fato_manifestos_dash FOR ETL_SISTEMA.dbo.vw_fato_manifestos_dash;
GO
