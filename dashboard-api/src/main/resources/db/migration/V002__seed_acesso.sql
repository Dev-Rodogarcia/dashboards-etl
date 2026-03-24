-- =============================================================
-- Seed: setores, permissoes, papeis e templates
-- Mapeamento identico ao access-control.json original
-- =============================================================

-- Setores
INSERT INTO acesso.setores (chave, nome, descricao, sistema) VALUES
('setor-admin',      N'Administração',  N'Área com acesso total ao sistema', 1),
('setor-logistica',  N'Logística',      N'Operação logística',               0),
('setor-financeiro', N'Financeiro',     N'Operação financeira',              0),
('setor-comercial',  N'Comercial',      N'Operação comercial',               0),
('setor-ti',         N'TI',            N'Monitoramento e sustentação',       0),
('setor-diretoria',  N'Diretoria',     N'Visão executiva',                   0);

-- Permissoes (catalogo — 11 permissoes legado)
INSERT INTO acesso.permissoes (chave, chave_legado, nome, descricao, recurso, acao, rota) VALUES
('dashboard.coletas.read',              'coletas',           N'Coletas',               N'Dashboard operacional de coletas',               'coletas',           'read', '/coletas'),
('dashboard.manifestos.read',           'manifestos',        N'Manifestos',            N'Dashboard operacional de manifestos',             'manifestos',        'read', '/manifestos'),
('dashboard.fretes.read',               'fretes',            N'Fretes',                N'Dashboard operacional de fretes',                 'fretes',            'read', '/fretes'),
('dashboard.tracking.read',             'tracking',          N'Localização de cargas', N'Dashboard de tracking e localização de cargas',   'tracking',          'read', '/tracking'),
('dashboard.faturas.read',              'faturas',           N'Faturas',               N'Dashboard financeiro de faturamento',             'faturas',           'read', '/faturas'),
('dashboard.faturas_por_cliente.read',  'faturasPorCliente', N'Faturas por Cliente',   N'Dashboard de faturamento por cliente',            'faturas_por_cliente','read', '/faturas-por-cliente'),
('dashboard.contas_a_pagar.read',       'contasAPagar',      N'Contas a pagar',        N'Dashboard financeiro de contas a pagar',          'contas_a_pagar',    'read', '/contas-a-pagar'),
('dashboard.cotacoes.read',             'cotacoes',          N'Cotações',              N'Dashboard comercial de cotações',                 'cotacoes',          'read', '/cotacoes'),
('dashboard.executivo.read',            'executivo',         N'Executivo',             N'Dashboard executivo consolidado',                 'executivo',         'read', '/executivo'),
('dashboard.etl_saude.read',            'etlSaude',          N'ETL Saúde',             N'Monitoramento e saúde do ETL',                    'etl_saude',         'read', '/etl-saude'),
('dashboard.dimensoes.read',            'dimensoes',         N'Dimensões e filtros',   N'Acesso às dimensões de apoio aos dashboards',     'dimensoes',         'read', NULL);

-- Papeis
INSERT INTO acesso.papeis (nome, descricao, nivel) VALUES
('admin_plataforma', N'Administrador com acesso total ao sistema',    100),
('admin_acesso',     N'Pode gerenciar usuários e setores',            50),
('usuario_comum',    N'Usuário padrão com permissões do setor',       10);

-- Templates de permissao por setor
-- Admin: todas as permissoes
INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s CROSS JOIN acesso.permissoes p
WHERE s.chave = 'setor-admin';

-- Logistica: coletas, manifestos, fretes, tracking, cotacoes, dimensoes
INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s CROSS JOIN acesso.permissoes p
WHERE s.chave = 'setor-logistica'
  AND p.chave_legado IN ('coletas','manifestos','fretes','tracking','cotacoes','dimensoes');

-- Financeiro: faturas, contasAPagar, cotacoes, dimensoes
INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s CROSS JOIN acesso.permissoes p
WHERE s.chave = 'setor-financeiro'
  AND p.chave_legado IN ('faturas','contasAPagar','cotacoes','dimensoes');

-- Comercial: cotacoes, dimensoes
INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s CROSS JOIN acesso.permissoes p
WHERE s.chave = 'setor-comercial'
  AND p.chave_legado IN ('cotacoes','dimensoes');

-- TI: etlSaude, dimensoes
INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s CROSS JOIN acesso.permissoes p
WHERE s.chave = 'setor-ti'
  AND p.chave_legado IN ('etlSaude','dimensoes');

-- Diretoria: executivo, dimensoes
INSERT INTO acesso.setor_permissao_templates (setor_id, permissao_id)
SELECT s.id, p.id
FROM acesso.setores s CROSS JOIN acesso.permissoes p
WHERE s.chave = 'setor-diretoria'
  AND p.chave_legado IN ('executivo','dimensoes');
