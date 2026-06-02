SET NOCOUNT ON;

IF DB_NAME() <> N'DASHBOARDS'
BEGIN
    THROW 51120, 'Execute esta validacao conectado ao banco DASHBOARDS.', 1;
END;

IF COL_LENGTH(N'acesso.usuarios', N'escopo_filiais_tipo') IS NULL
BEGIN
    THROW 51121, 'Coluna acesso.usuarios.escopo_filiais_tipo nao encontrada. Execute a V011.', 1;
END;

IF OBJECT_ID(N'acesso.usuario_filiais_permitidas', N'U') IS NULL
BEGIN
    THROW 51122, 'Tabela acesso.usuario_filiais_permitidas nao encontrada. Execute a V011.', 1;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_usuarios_escopo_filiais_tipo'
      AND parent_object_id = OBJECT_ID(N'acesso.usuarios', N'U')
)
BEGIN
    THROW 51123, 'Constraint CK_usuarios_escopo_filiais_tipo nao encontrada.', 1;
END;

SELECT
    DB_NAME() AS database_name,
    COL_LENGTH(N'acesso.usuarios', N'escopo_filiais_tipo') AS tamanho_coluna_escopo,
    OBJECT_ID(N'acesso.usuario_filiais_permitidas', N'U') AS object_id_usuario_filiais,
    COUNT_BIG(*) AS usuarios_total,
    SUM(CASE WHEN escopo_filiais_tipo = 'HERDAR_SETOR' THEN 1 ELSE 0 END) AS usuarios_herdar_setor,
    SUM(CASE WHEN escopo_filiais_tipo = 'TODAS' THEN 1 ELSE 0 END) AS usuarios_todas,
    SUM(CASE WHEN escopo_filiais_tipo = 'SELECIONADAS' THEN 1 ELSE 0 END) AS usuarios_selecionadas
FROM acesso.usuarios;

SELECT
    COUNT_BIG(*) AS vinculos_usuario_filial
FROM acesso.usuario_filiais_permitidas;
