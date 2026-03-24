# Relatorio Consolidado da Refatoracao

Data de consolidacao: 2026-03-22

## 1. Contexto

Esta aplicacao deixou de operar sobre paineis simplificados e views ficticias e passou a ser guiada pelo catalogo real de views do SQL Server.

As fontes de verdade usadas ao longo da refatoracao foram:

- `docs/relatorio-bi-catalogo-views.md`
- `docs/relatorio-bi-dashboards-logistica.md`
- `database/views/*.sql`
- plano original de execucao em `C:\Users\lucas\.claude\plans\misty-purring-fox.md` (fora do repositorio)

O objetivo foi alinhar frontend e backend com os dados reais, ampliar os dashboards para cobrir os dominios operacionais e financeiros e fechar a camada de autenticacao/autorizacao por perfil.

## 2. Resultado final em alto nivel

O repositorio atual passou a ter:

- 9 dashboards protegidos por permissao:
  - Coletas
  - Manifestos
  - Fretes
  - Tracking
  - Faturas
  - Contas a pagar
  - Cotacoes
  - Executivo
  - ETL Saude
- filtros compartilhados por URL
- endpoints por dominio no backend
- integracao com views de dimensao para filtros
- autenticacao JWT
- autorizacao por setor/perfil com area administrativa
- CRUD de setores
- CRUD de usuarios

## 3. O que foi feito no backend

### 3.1 Alinhamento com as views reais

As entities JPA foram ajustadas para refletir os aliases reais das views do SQL Server, em vez de nomes internos antigos que nao existiam no banco. Esse ajuste eliminou a classe de erro que gerava `500` por mapeamento incorreto entre entidade e view.

Tambem foi configurada a estrategia fisica de nomes do Hibernate para preservar os nomes explicitos de colunas definidos nas entities.

### 3.2 Padrao de endpoints por dominio

O backend passou a expor contratos consistentes para os dashboards, com combinacoes de:

- `overview`
- `serie`
- `breakdowns` especializados
- `tabela`

Os endpoints foram organizados em `/api/painel/*`, cobrindo os dominios:

- `coletas`
- `manifestos`
- `fretes`
- `tracking`
- `faturas`
- `contas-a-pagar`
- `cotacoes`
- `executivo`
- `etl-saude`

### 3.3 Filtros e validacao de periodo

Foi consolidado um contrato de filtros com:

- `dataInicio`
- `dataFim`
- filtros dimensionais serializados na URL como `f.<chave>`

A validacao de janela maxima de 90 dias foi centralizada para evitar divergencia entre paginas e services.

### 3.4 Dimensoes para apoio de filtro

Foram expostos endpoints leves para:

- filiais
- clientes
- motoristas
- veiculos
- plano de contas
- usuarios

Essas dimensoes alimentam selects assincronos no frontend e podem ser consumidas por multiplos dashboards.

### 3.5 Faturas com duas fontes

O dashboard de faturas deixou de depender de um unico grao de dados e passou a considerar a separacao entre:

- visao operacional
- visao financeira

Isso permitiu modelar melhor:

- faturado x recebido
- aging
- top clientes
- status do processo
- reconciliacao

### 3.6 Autenticacao e seguranca

Foi consolidada uma seguranca stateless com JWT:

- `POST /api/auth/login`
- `GET /api/auth/me`
- endpoint removido por exposição indevida de contas

O token carrega o usuario autenticado e as authorities sao recalculadas no backend com base no estado atual do controle de acesso, evitando depender de permissoes congeladas no JWT.

### 3.7 Controle de acesso por setor

Foi implementado um sistema de ACL orientado a setor/perfil:

- catalogo central de permissoes
- setores com mapa de permissoes
- usuarios vinculados a setores
- administradores com acesso total

O catalogo atual de permissoes contempla:

- `coletas`
- `manifestos`
- `fretes`
- `tracking`
- `faturas`
- `contasAPagar`
- `cotacoes`
- `executivo`
- `etlSaude`
- `dimensoes`

As permissoes ficam centralizadas em `PermissaoCatalogo` e sao aplicadas com `@PreAuthorize` nos controllers.

### 3.8 Area admin no backend

Foi criado um modulo administrativo em `/api/admin/acesso` com:

- `GET /catalogo-permissoes`
- `GET /setores`
- `POST /setores`
- `PUT /setores/{id}`
- `DELETE /setores/{id}`
- `GET /usuarios`
- `POST /usuarios`
- `PUT /usuarios/{id}`
- `DELETE /usuarios/{id}`

Regras de negocio importantes:

- nome de setor unico
- login unico
- email unico
- nao excluir setor de sistema
- nao excluir setor com usuarios vinculados
- obrigatoriedade de manter ao menos um admin ativo

### 3.9 Persistencia do ACL

O controle de acesso foi implementado com armazenamento em JSON para o modo atual do projeto. O arquivo eh controlado por:

- `acl.storage-file`
- legado JSON desabilitado por padrão; habilite migração só de forma temporária e explícita

Quando o storage nao existe, o sistema semeia perfis e usuarios padrao.

Usuarios de desenvolvimento semeados:

- credenciais fixas removidas; ambientes devem usar bootstrap explícito e rotação de segredo

## 4. O que foi feito no frontend

### 4.1 Estrutura de navegacao

O frontend foi reorganizado em torno de:

- `LayoutPainel`
- `BarraLateral`
- `Cabecalho`
- `RotaProtegida`
- paginas por dominio

As rotas estao centralizadas em `dashboard-ui/src/App.tsx` e agora usam permissao por rota, nao listas hardcoded de setores.

### 4.2 Dashboards protegidos

As paginas principais passaram a responder ao contexto de autenticacao e filtro:

- `ColetasPage`
- `ManifestosPage`
- `FretesPage`
- `TrackingPage`
- `FaturasPage`
- `ContasAPagarPage`
- `CotacoesPage`
- `ExecutivoPage`
- `EtlSaudePage`

### 4.3 Filtros persistidos em URL

O `FiltroContext` passou a usar `useSearchParams`, permitindo:

- bookmarks
- compartilhamento de URL
- restauracao de estado ao recarregar
- integracao direta entre filtro visual e request da API

Contrato atual:

- `dataInicio`
- `dataFim`
- filtros adicionais como `f.filial`, `f.cliente`, `f.status` e equivalentes

### 4.4 Tipos e contratos

Foram consolidados tipos por dominio e tipos de autenticacao/acesso, separando:

- sessao do usuario
- setor do usuario
- mapa de permissoes
- payloads de setor
- payloads de usuario
- catalogo de permissoes

### 4.5 Componentes compartilhados

Foram integrados componentes de apoio para uso repetido nos dashboards, como:

- cards de KPI
- wrappers de grafico
- barra de filtros
- seletor de periodo
- selects assincronos
- tabelas analiticas
- empty state
- exportacao CSV

### 4.6 Login e sessao

O fluxo de autenticacao no frontend passou a:

- persistir sessao localmente
- revalidar a sessao em `/api/auth/me`
- popular o login com usuarios disponiveis em dev
- redirecionar o usuario para a primeira rota que ele pode acessar

### 4.7 Autorizacao no frontend

Foi implementado um controle de acesso em varios niveis:

- `RotaProtegida` para bloqueio de rota
- `PermissionGate` para renderizacao condicional
- sidebar dinamica baseada nas permissoes do setor
- rotas admin visiveis apenas para administradores
- fallback de `403` para `/acesso-negado`

### 4.8 Area admin no frontend

Foram criadas duas paginas administrativas:

- `/admin/setores`
- `/admin/usuarios`

Capacidades da area admin:

- criar, editar e excluir setores
- configurar permissoes por checkbox
- criar, editar e excluir usuarios
- trocar setor de usuario
- promover usuario para admin
- ativar/desativar usuario

## 5. Limpeza e remocao de legado

O codigo antigo baseado em paineis ficticios foi removido do caminho principal do produto.

Foram eliminados artefatos do modelo legado, incluindo:

- paginas antigas de painel
- servicos antigos do painel
- hooks antigos do painel
- tipos antigos do painel
- componentes antigos de grafico do painel
- views SQL ficticias antigas que nao representavam o catalogo real

## 6. Arquivos centrais da refatoracao

Arquivos principais para consulta:

- `dashboard-ui/src/App.tsx`
- `dashboard-ui/src/contexts/AutenticacaoContext.tsx`
- `dashboard-ui/src/contexts/FiltroContext.tsx`
- `dashboard-ui/src/components/layout/RotaProtegida.tsx`
- `dashboard-ui/src/components/layout/BarraLateral.tsx`
- `dashboard-ui/src/components/PermissionGate.tsx`
- `dashboard-ui/src/pages/LoginPage.tsx`
- `dashboard-ui/src/pages/AdminSetoresPage.tsx`
- `dashboard-ui/src/pages/AdminUsuariosPage.tsx`
- `dashboard-ui/src/utils/accessControl.ts`
- `dashboard-ui/src/types/access.ts`
- `dashboard-api/src/main/java/com/dashboard/api/security/PermissaoCatalogo.java`
- `dashboard-api/src/main/java/com/dashboard/api/security/AcessoSeguranca.java`
- `dashboard-api/src/main/java/com/dashboard/api/security/FiltroValidacaoJwt.java`
- `dashboard-api/src/main/java/com/dashboard/api/controller/AutenticacaoController.java`
- `dashboard-api/src/main/java/com/dashboard/api/controller/AdminAcessoController.java`
- `dashboard-api/src/main/java/com/dashboard/api/service/ControleAcessoService.java`

## 7. Validacoes realizadas

Validacoes de qualidade executadas na etapa final:

- `dashboard-ui`: `npm run lint`
- `dashboard-ui`: `npm run build`
- `dashboard-api`: `./mvnw -q compile`
- `dashboard-api`: `./mvnw test`

Validacoes realizadas ao longo da refatoracao de dados:

- smoke tests em endpoints do backend com dados reais do SQL Server apos correcao de mapeamentos e aliases
- verificacao dos endpoints usados pelos dashboards principais

## 8. Limitacoes e observacoes

Pontos importantes para manutencao:

- o controle de acesso atual usa storage JSON, nao banco relacional
- o JWT nao embute o mapa de permissoes; as authorities sao recalculadas a cada request
- o frontend ainda gera bundle grande em build de producao e pode se beneficiar de code splitting
- a validacao HTTP final do fluxo completo de ACL deve ser reexecutada sempre que o backend estiver em execucao local, para confirmar o comportamento vivo de `401` e `403`

## 9. Como operar no dia a dia

### Login

- não documentar nem distribuir credenciais padrão
- usar usuarios comuns para testar o comportamento por setor

### Gestao de acesso

1. Entrar como admin
2. Abrir `/admin/setores`
3. Criar ou editar um setor
4. Marcar as permissoes desejadas
5. Abrir `/admin/usuarios`
6. Vincular o usuario ao setor correto

### Resultado esperado

- sidebar mostra apenas o que o perfil pode acessar
- rotas bloqueadas redirecionam para `acesso-negado`
- endpoints do backend tambem negam acesso por `403`
- admin tem acesso total ao sistema

## 10. Resumo executivo

O projeto foi convertido de um painel simplificado e parcialmente ficticio para uma plataforma de dashboards orientada pelo catalogo real de views, com frontend e backend alinhados, filtros consistentes, dashboards por dominio, integracao com dimensoes e um sistema completo de controle de acesso por setor com area administrativa.
