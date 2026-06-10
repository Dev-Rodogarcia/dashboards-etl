IF DB_ID(N'ETL_SISTEMA') IS NULL
    THROW 51270, 'Database fonte ETL_SISTEMA nao encontrado para criacao dos synonyms do Dashboard.', 1;
GO

IF OBJECT_ID(N'dbo.vw_faturas_por_cliente_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_faturas_por_cliente_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_faturas_por_cliente_powerbi') DROP SYNONYM dbo.vw_faturas_por_cliente_powerbi;
GO
CREATE SYNONYM dbo.vw_faturas_por_cliente_powerbi FOR ETL_SISTEMA.dbo.vw_faturas_por_cliente_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_fretes_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_fretes_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_fretes_powerbi') DROP SYNONYM dbo.vw_fretes_powerbi;
GO
CREATE SYNONYM dbo.vw_fretes_powerbi FOR ETL_SISTEMA.dbo.vw_fretes_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_coletas_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_coletas_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_coletas_powerbi') DROP SYNONYM dbo.vw_coletas_powerbi;
GO
CREATE SYNONYM dbo.vw_coletas_powerbi FOR ETL_SISTEMA.dbo.vw_coletas_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_cotacoes_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_cotacoes_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_cotacoes_powerbi') DROP SYNONYM dbo.vw_cotacoes_powerbi;
GO
CREATE SYNONYM dbo.vw_cotacoes_powerbi FOR ETL_SISTEMA.dbo.vw_cotacoes_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_contas_a_pagar_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_contas_a_pagar_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_contas_a_pagar_powerbi') DROP SYNONYM dbo.vw_contas_a_pagar_powerbi;
GO
CREATE SYNONYM dbo.vw_contas_a_pagar_powerbi FOR ETL_SISTEMA.dbo.vw_contas_a_pagar_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_localizacao_cargas_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_localizacao_cargas_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_localizacao_cargas_powerbi') DROP SYNONYM dbo.vw_localizacao_cargas_powerbi;
GO
CREATE SYNONYM dbo.vw_localizacao_cargas_powerbi FOR ETL_SISTEMA.dbo.vw_localizacao_cargas_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_manifestos_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_manifestos_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_manifestos_powerbi') DROP SYNONYM dbo.vw_manifestos_powerbi;
GO
CREATE SYNONYM dbo.vw_manifestos_powerbi FOR ETL_SISTEMA.dbo.vw_manifestos_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_bi_monitoramento', N'V') IS NOT NULL DROP VIEW dbo.vw_bi_monitoramento;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_bi_monitoramento') DROP SYNONYM dbo.vw_bi_monitoramento;
GO
CREATE SYNONYM dbo.vw_bi_monitoramento FOR ETL_SISTEMA.dbo.vw_bi_monitoramento;
GO

IF OBJECT_ID(N'dbo.vw_inventario_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_inventario_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_inventario_powerbi') DROP SYNONYM dbo.vw_inventario_powerbi;
GO
CREATE SYNONYM dbo.vw_inventario_powerbi FOR ETL_SISTEMA.dbo.vw_inventario_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_sinistros_powerbi', N'V') IS NOT NULL DROP VIEW dbo.vw_sinistros_powerbi;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_sinistros_powerbi') DROP SYNONYM dbo.vw_sinistros_powerbi;
GO
CREATE SYNONYM dbo.vw_sinistros_powerbi FOR ETL_SISTEMA.dbo.vw_sinistros_powerbi;
GO

IF OBJECT_ID(N'dbo.vw_raster_sm_transit_time', N'V') IS NOT NULL DROP VIEW dbo.vw_raster_sm_transit_time;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_raster_sm_transit_time') DROP SYNONYM dbo.vw_raster_sm_transit_time;
GO
CREATE SYNONYM dbo.vw_raster_sm_transit_time FOR ETL_SISTEMA.dbo.vw_raster_sm_transit_time;
GO

IF OBJECT_ID(N'dbo.vw_dim_filiais', N'V') IS NOT NULL DROP VIEW dbo.vw_dim_filiais;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_dim_filiais') DROP SYNONYM dbo.vw_dim_filiais;
GO
CREATE SYNONYM dbo.vw_dim_filiais FOR ETL_SISTEMA.dbo.vw_dim_filiais;
GO

IF OBJECT_ID(N'dbo.vw_dim_clientes', N'V') IS NOT NULL DROP VIEW dbo.vw_dim_clientes;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_dim_clientes') DROP SYNONYM dbo.vw_dim_clientes;
GO
CREATE SYNONYM dbo.vw_dim_clientes FOR ETL_SISTEMA.dbo.vw_dim_clientes;
GO

IF OBJECT_ID(N'dbo.vw_dim_veiculos', N'V') IS NOT NULL DROP VIEW dbo.vw_dim_veiculos;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_dim_veiculos') DROP SYNONYM dbo.vw_dim_veiculos;
GO
CREATE SYNONYM dbo.vw_dim_veiculos FOR ETL_SISTEMA.dbo.vw_dim_veiculos;
GO

IF OBJECT_ID(N'dbo.vw_dim_motoristas', N'V') IS NOT NULL DROP VIEW dbo.vw_dim_motoristas;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_dim_motoristas') DROP SYNONYM dbo.vw_dim_motoristas;
GO
CREATE SYNONYM dbo.vw_dim_motoristas FOR ETL_SISTEMA.dbo.vw_dim_motoristas;
GO

IF OBJECT_ID(N'dbo.vw_dim_planocontas', N'V') IS NOT NULL DROP VIEW dbo.vw_dim_planocontas;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_dim_planocontas') DROP SYNONYM dbo.vw_dim_planocontas;
GO
CREATE SYNONYM dbo.vw_dim_planocontas FOR ETL_SISTEMA.dbo.vw_dim_planocontas;
GO

IF OBJECT_ID(N'dbo.vw_dim_usuarios', N'V') IS NOT NULL DROP VIEW dbo.vw_dim_usuarios;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'vw_dim_usuarios') DROP SYNONYM dbo.vw_dim_usuarios;
GO
CREATE SYNONYM dbo.vw_dim_usuarios FOR ETL_SISTEMA.dbo.vw_dim_usuarios;
GO

IF OBJECT_ID(N'dbo.fato_fretes_faturamento', N'V') IS NOT NULL DROP VIEW dbo.fato_fretes_faturamento;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'fato_fretes_faturamento') DROP SYNONYM dbo.fato_fretes_faturamento;
GO
CREATE SYNONYM dbo.fato_fretes_faturamento FOR ETL_SISTEMA.dbo.fato_fretes_faturamento;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_faturas', N'V') IS NOT NULL DROP VIEW dbo.fato_gestao_vista_faturas;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'fato_gestao_vista_faturas') DROP SYNONYM dbo.fato_gestao_vista_faturas;
GO
CREATE SYNONYM dbo.fato_gestao_vista_faturas FOR ETL_SISTEMA.dbo.fato_gestao_vista_faturas;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_fretes', N'V') IS NOT NULL DROP VIEW dbo.fato_gestao_vista_fretes;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'fato_gestao_vista_fretes') DROP SYNONYM dbo.fato_gestao_vista_fretes;
GO
CREATE SYNONYM dbo.fato_gestao_vista_fretes FOR ETL_SISTEMA.dbo.fato_gestao_vista_fretes;
GO

IF OBJECT_ID(N'dbo.fato_gestao_vista_coletores', N'V') IS NOT NULL DROP VIEW dbo.fato_gestao_vista_coletores;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'fato_gestao_vista_coletores') DROP SYNONYM dbo.fato_gestao_vista_coletores;
GO
CREATE SYNONYM dbo.fato_gestao_vista_coletores FOR ETL_SISTEMA.dbo.fato_gestao_vista_coletores;
GO

IF OBJECT_ID(N'dbo.dim_calendario', N'V') IS NOT NULL DROP VIEW dbo.dim_calendario;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'dim_calendario') DROP SYNONYM dbo.dim_calendario;
GO
CREATE SYNONYM dbo.dim_calendario FOR ETL_SISTEMA.dbo.dim_calendario;
GO

IF OBJECT_ID(N'dbo.log_extracoes', N'V') IS NOT NULL DROP VIEW dbo.log_extracoes;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'log_extracoes') DROP SYNONYM dbo.log_extracoes;
GO
CREATE SYNONYM dbo.log_extracoes FOR ETL_SISTEMA.dbo.log_extracoes;
GO

IF OBJECT_ID(N'dbo.raster_viagens', N'V') IS NOT NULL DROP VIEW dbo.raster_viagens;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'raster_viagens') DROP SYNONYM dbo.raster_viagens;
GO
CREATE SYNONYM dbo.raster_viagens FOR ETL_SISTEMA.dbo.raster_viagens;
GO

IF OBJECT_ID(N'dbo.raster_viagem_paradas', N'V') IS NOT NULL DROP VIEW dbo.raster_viagem_paradas;
IF EXISTS (SELECT 1 FROM sys.synonyms WHERE schema_id = SCHEMA_ID(N'dbo') AND name = N'raster_viagem_paradas') DROP SYNONYM dbo.raster_viagem_paradas;
GO
CREATE SYNONYM dbo.raster_viagem_paradas FOR ETL_SISTEMA.dbo.raster_viagem_paradas;
GO
