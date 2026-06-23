SET NOCOUNT ON;

IF DB_NAME() IN (N'ETL_SISTEMA', N'SATELITE_TMS_AUDITORIA', N'master', N'model', N'msdb', N'tempdb')
BEGIN
    THROW 51110, 'Execute este preflight conectado ao banco proprio do Dashboard.', 1;
END;

SELECT
    DB_NAME() AS database_name,
    SUSER_SNAME() AS login_name,
    USER_NAME() AS database_user,
    CASE WHEN SCHEMA_ID(N'acesso') IS NULL THEN 0 ELSE 1 END AS schema_acesso_existe,
    CASE WHEN OBJECT_ID(N'acesso.usuarios', N'U') IS NULL THEN 0 ELSE 1 END AS tabela_usuarios_existe,
    CASE WHEN COL_LENGTH(N'acesso.usuarios', N'escopo_filiais_tipo') IS NULL THEN 0 ELSE 1 END AS coluna_escopo_existe,
    CASE WHEN OBJECT_ID(N'acesso.usuario_filiais_permitidas', N'U') IS NULL THEN 0 ELSE 1 END AS tabela_usuario_filiais_existe,
    COALESCE(HAS_PERMS_BY_NAME(N'acesso.usuarios', N'OBJECT', N'ALTER'), 0) AS pode_alterar_usuarios,
    COALESCE(HAS_PERMS_BY_NAME(DB_NAME(), N'DATABASE', N'CREATE TABLE'), 0) AS pode_criar_tabela;

IF OBJECT_ID(N'acesso.usuarios', N'U') IS NULL
BEGIN
    THROW 51111, 'Tabela acesso.usuarios nao existe. Aplique as migrations anteriores antes da V011.', 1;
END;

IF COALESCE(HAS_PERMS_BY_NAME(N'acesso.usuarios', N'OBJECT', N'ALTER'), 0) <> 1
   OR COALESCE(HAS_PERMS_BY_NAME(DB_NAME(), N'DATABASE', N'CREATE TABLE'), 0) <> 1
BEGIN
    THROW 51112, 'Usuario conectado nao tem permissao suficiente para aplicar a V011.', 1;
END;
