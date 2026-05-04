-- =============================================================
-- Corrige textos corrompidos no modulo de acesso sem alterar IDs
-- ou chaves de negocio. Aplicacao manual/operacional.
-- =============================================================

UPDATE acesso.setores
SET
    nome = N'Administração',
    descricao = N'Área com acesso total ao sistema'
WHERE chave = 'setor-admin';

UPDATE acesso.setores
SET
    nome = N'Logística',
    descricao = N'Operação logística'
WHERE chave = 'setor-logistica';

UPDATE acesso.setores
SET
    nome = N'Financeiro',
    descricao = N'Operação financeira'
WHERE chave = 'setor-financeiro';

UPDATE acesso.setores
SET
    nome = N'Comercial',
    descricao = N'Operação comercial'
WHERE chave = 'setor-comercial';

UPDATE acesso.setores
SET
    nome = N'TI',
    descricao = N'Monitoramento e sustentação'
WHERE chave = 'setor-ti';

UPDATE acesso.setores
SET
    nome = N'Diretoria',
    descricao = N'Visão executiva'
WHERE chave = 'setor-diretoria';

UPDATE acesso.papeis
SET descricao = N'Pode gerenciar usuários e setores'
WHERE nome = 'admin_acesso';

UPDATE acesso.papeis
SET descricao = N'Usuário padrão com permissões do setor'
WHERE nome = 'usuario_comum';

UPDATE acesso.permissoes
SET
    nome = N'Localização de cargas',
    descricao = N'Dashboard de tracking e localização de cargas'
WHERE chave_legado = 'tracking';

UPDATE acesso.permissoes
SET
    nome = N'Cotações',
    descricao = N'Dashboard comercial de cotações'
WHERE chave_legado = 'cotacoes';

UPDATE acesso.permissoes
SET
    nome = N'ETL Saúde',
    descricao = N'Monitoramento e saúde do ETL'
WHERE chave_legado = 'etlSaude';

UPDATE acesso.permissoes
SET
    nome = N'Dimensões e filtros',
    descricao = N'Acesso às dimensões de apoio aos dashboards'
WHERE chave_legado = 'dimensoes';
