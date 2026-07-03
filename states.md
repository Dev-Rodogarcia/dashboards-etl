# Estado Atual do Sistema

## Stack Tecnológica
- Monorepo de portal web com backend Java 17/Spring Boot 3.2.5 e frontend React 19/TypeScript/Vite.
- Backend: Spring Web, Spring Security, Spring Data JPA, Spring Data LDAP, Bean Validation, Actuator, Flyway, Flyway SQL Server, spring-dotenv, JJWT, Apache POI, BouncyCastle/Argon2, Microsoft JDBC Driver e H2 em testes.
- Frontend: React Router DOM v6, TanStack Query v5, Axios, Apache ECharts, Tailwind CSS v4, Radix UI, lucide-react, framer-motion, next-themes, Vitest e ESLint.
- Banco próprio: SQL Server `DASHBOARDS` em produção e `DASHBOARDS_DEV` em desenvolvimento.
- Fonte analítica externa: SQL Server `ETL_SISTEMA`/`esl_cloud`, consumido em leitura por views, fatos e synonyms publicados pelo ETL.
- Operação Windows por `iniciar-dev.bat`, `iniciar-prod.bat`, Maven Wrapper, scripts em `scripts/` e migrations em `database/`.

## Arquitetura e Padrões
- Arquitetura em camadas Spring MVC no backend e SPA React no frontend.
- Backend: `controller` expõe fronteiras HTTP; `service` orquestra regras; `repository` concentra SQL/JPA; `dto` define contratos; `model` representa entidades; `security` concentra JWT, API key e rate limit; `config` define infraestrutura; `policy` guarda regras reutilizáveis; `util` contém helpers puros.
- Frontend: `src/App.tsx` define rotas lazy e proteção de acesso; `contexts` guarda sessão/filtros/cabeçalho; `api` centraliza Axios e endpoints; `hooks/queries` encapsula React Query; `pages` monta dashboards; `components/shared` padroniza filtros, cards, tabelas, exportação, gráficos e estados.
- Modais animados com múltiplos filhos diretos em `AnimatePresence` devem declarar `key` explícita e estável por elemento; o modal de justificativa de Horário de Corte identifica backdrop e diálogo pela SM selecionada para evitar chaves vazias no React/Framer Motion.
- Banco `DASHBOARDS` é gerido exclusivamente por Flyway em `database/migrations`, com baseline 22 e migrations atuais até `V050__adicionar_soft_delete_justificativas.sql`.
- Hibernate roda com `spring.jpa.hibernate.ddl-auto=none`; DDL em runtime por Java é proibido.
- O portal é consumidor read-only do ETL. O backend deve consultar objetos analíticos por nomes simples (`dbo.vw_*`, `dbo.fato_*`, `dbo.dim_*`), sem hardcode de database.
- Padrões obrigatórios: push-down computation no SQL Server, filtros sargable, paginação/exportação em SQL, DTOs pequenos, validação explícita de período e paridade entre cálculo de KPI e dicionários do frontend.
- Produção e desenvolvimento são isolados por `.env`, profiles e validações que impedem DEV de conectar no banco `DASHBOARDS`.
- Exportações CSV baseadas em records Java usam `CsvExportWriter`; quando um DTO precisa de cabeçalho amigável sem alterar o contrato JSON, o componente pode usar `@CsvColumn`, como em `HorarioCorteRowDTO.justificativa` exportado como `Justificativa`.
- Dados de negócio, justificativas e auditoria usam exclusão lógica obrigatória; hard delete/`DELETE FROM` físico é proibido. Entidades JPA podem delegar o soft delete ao Hibernate com `@SQLDelete`/`@SQLRestriction`, e queries JDBC nativas devem filtrar registros inativos explicitamente.
- A série gráfica de Performance de Entrega em Gestão à Vista usa o enum backend `NivelVisaoPerformance` (`RESPONSAVEL`, `REGIAO`, `CIDADE`) como contrato estrito de visão. O DTO expõe `label`, `filtro`, `visao`, totais físicos e percentual; o SQL seleciona e agrupa apenas colunas mapeadas pelo enum.
- Componentes de seção de Gestão à Vista podem repassar `chartActions` e `chartEvents` ao `ChartWrapper`, permitindo breadcrumbs e drill-down em Apache ECharts sem duplicar wrappers visuais.
- `ChartWrapper` tipa e encaminha `onEvents` diretamente ao `echarts-for-react`; dashboards com drill-down devem passar handlers de clique pelo wrapper, usar a seta do breadcrumb para retornar à raiz e deixar níveis indisponíveis com `disabled`/`cursor-not-allowed`.

## Fluxo de Dados e Integrações
- Fluxo web: React -> hooks React Query -> `src/api/endpoints` -> `clienteAxios` -> controllers Spring -> services -> repositories SQL/JPA -> SQL Server -> DTOs -> cards/gráficos/tabelas.
- Autenticação: `/api/auth/login`, `/api/auth/me`, `/api/auth/alterar-senha`, `/api/auth/refresh` e `/api/auth/logout`.
- Dashboards principais: `/api/painel/coletas`, `/manifestos`, `/fretes`, `/tracking`, `/performance`, `/faturas-por-cliente`, `/contas-a-pagar`, `/cotacoes`, `/executivo`, `/etl-saude`, `/integracoes` e `/indicadores-gestao-a-vista`.
- Endpoints de apoio: `/api/dimensoes`, `/api/admin/acesso`, `/api/admin/acesso/usuarios/importacao`, `/api/kpi-goals`, `/api/etl/quarentena`, paginação `/api/painel/*/tabela/paginada`, exportação `/api/painel/*/exportacao` e justificativas de Horário de Corte em `/api/painel/indicadores-gestao-a-vista/horarios-corte/justificativas`.
- Frontend possui rotas protegidas para Coletas, Manifestos, Faturamento/Fretes, Performance, Tracking, Faturas por Cliente, Contas a Pagar, Cotações, Indicadores de Gestão à Vista, Executivo, ETL Saúde, Integrações e Administração.
- Objetos analíticos lidos do ETL incluem `dbo.vw_coletas_powerbi`, `dbo.vw_fretes_powerbi`, `dbo.vw_manifestos_powerbi`, `dbo.vw_localizacao_cargas_powerbi`, `dbo.vw_contas_a_pagar_powerbi`, `dbo.vw_cotacoes_powerbi`, `dbo.vw_sinistros_powerbi`, `dbo.vw_fato_manifestos_dash`, `dbo.fato_fretes_faturamento`, `dbo.fato_gestao_vista_fretes`, `dbo.fato_gestao_vista_coletores`, `dbo.fato_gestao_vista_faturas`, `dbo.fato_gestao_vista_manifestos`, `dbo.dim_calendario` e `dbo.vw_dim_*`.
- Estado próprio em `DASHBOARDS`: schema `acesso` para usuários, papéis, permissões, setores, filiais permitidas, refresh tokens, audit logs, metas de KPI, metas de fretes, metas de custo de manifestos e comunicados; `dbo.viagem_justificativas` para justificativas de viagens Raster usadas pelo indicador de Horário de Corte.
- Integração adicional: `IntegracaoSateliteClient` aponta para `APP_INTEGRATION_SATELITE_URL` com padrão `http://127.0.0.1:19090`.
- Builds: dev usa backend 5011 e frontend 5174; produção usa backend 5010 e frontend 5173, com Cloudflare Tunnel previsto.
- Gestão à Vista: `/api/painel/indicadores-gestao-a-vista/performance-entrega/serie` exige `visao` e aceita `responsavelFiltro`/`regiaoFiltro` para drill-down. O frontend envia esses parâmetros por `indicadoresGestaoAVistaServico.ts`, inclui todos no `queryKey` do TanStack Query e atualiza o gráfico ao clicar em barras.
- A rota `/performance` mantém drill-down por `drillNivel`, `drillResponsavel` e `drillRegiao` nos query params; esses valores entram no `queryKey` de `usePerformanceDrilldown`, nos filtros da tabela e nos handlers de clique do gráfico.

## Regras de Negócio Consolidadas
- O Dashboard não é dono do banco analítico; alterações em views/tabelas/fatos do ETL devem ser feitas em `etl-extracao-dados`.
- É proibido hardcode de `ETL_SISTEMA` no Java/JPA/queries do Dashboard; use nomes simples e synonyms/migrations do banco próprio quando necessário.
- Toda alteração estrutural do banco próprio deve estar em Flyway e refletida em setup/validation quando aplicável.
- O usuário de runtime deve ter privilégio mínimo; permissões DDL pertencem ao processo controlado de migration.
- Consultas de dashboard exigem período válido, `dataInicio <= dataFim` e máximo de 365 dias.
- Para colunas `DATETIMEOFFSET`, usar `PeriodoOffsetDateTimeHelper` com zona `America/Sao_Paulo`, início inclusivo e fim exclusivo.
- Agregações, somas, contagens, rankings, filtros volumosos, distinct, paginação e exportação devem ser executados no SQL Server.
- ACL: permissões efetivas combinam catálogo ativo, template do setor e overrides individuais `GRANT`/`DENY`; admins de acesso, admin plataforma e desenvolvedor têm privilégios elevados.
- Rotas backend usam `@PreAuthorize("@acessoSeguranca...")`; frontend espelha permissões em `src/utils/accessControl.ts`.
- Escopo de filiais por usuário aceita `HERDAR_SETOR`, `TODAS` e `SELECIONADAS`; seleção vazia não é válida em `SELECIONADAS`.
- Sessão: access token JWT fica em memória no frontend; refresh token rotativo fica em cookie HttpOnly; 401 tenta refresh silencioso e encerra sessão se falhar.
- Política de senha: mínimo 12 caracteres, com maiúscula, minúscula, número e símbolo; hashes novos usam Argon2id, com upgrade de hash legado quando aplicável.
- Login bloqueia após 5 falhas por 15 minutos; troca de senha revoga todos os refresh tokens do usuário.
- Rate limit separa login, API geral e exportação; `/api/interno/**` exige `X-API-KEY`.
- Metas de fretes: branch `GLOBAL` é `NULL`, ano 2000-2100, mês 1-12 e meta não negativa; replicação só ocorre se destino estiver vazio e origem tiver metas.
- Metas de KPIs de Gestão à Vista: indicadores válidos são `delivery_performance`, `collector_usage`, `cargo_cubage`, `cargo_indemnity` e `cutoff_time`; metas entre 0 e 100; competência normalizada para primeiro dia do mês; override igual ao global é removido.
- Metas de custo de manifestos: branch `GLOBAL` vira `NULL`, contrato padrão `Geral/geral`, `classification_key` opcional e normalizada, custo não negativo; filtros sem dimensão orçamentária tornam orçamento inaplicável.
- Performance de Entrega em Gestão à Vista: o ranking/drill-down percorre `RESPONSAVEL -> REGIAO -> CIDADE`. A fonte é `dbo.fato_gestao_vista_fretes`, filtrada por `indicador_codigo = 'PE'`, `data_referencia >= :dataInicio`, `data_referencia < :dataFimExclusivo`, `is_linha_valida_indicador = 1`, `excluido_na_origem = 0` e escopo de filiais. Responsável e região selecionados viram filtros parametrizados; agrupamento, totalizações e Top 50 são calculados no SQL Server.
- Horário de Corte: viagens Raster são avaliadas pela saída real contra o horário de corte da rota com tolerância de 10 minutos; SMs com justificativa ativa em `dbo.viagem_justificativas` contam como no horário, expõem o texto da justificativa na tabela/exportação CSV e podem ter a justificativa inativada por DELETE HTTP idempotente. O banco preserva o registro com `ativo = 0`, e as queries nativas filtram `vj.ativo = 1` para retornar a SM ao status original de contabilização.
- Alteração de KPI deve atualizar `frontend/src/constants/kpiDictionary.ts`; alteração de regra/fonte de gráfico deve atualizar `frontend/src/constants/chartDictionary.ts`.
- Produção é controlada por operador humano: não executar `iniciar-prod.bat`, não reiniciar processos e não liberar portas 5010/5173 por automação.
- Arquivos, SQL e seeds devem permanecer em UTF-8 e sem mojibake.

## Protocolo de Planejamento de Requisições
- Antes de iniciar qualquer planejamento ou escrita de código, a IA DEVE OBRIGATORIAMENTE ler `AGENTS.md` do projeto local e `CONTEXTO_GLOBAL.md`.
- O `CONTEXTO_GLOBAL.md` dita as regras do ecossistema e o `AGENTS.md` dita as regras locais. Falhar em ler e aplicar essas regras resulta em quebra arquitetural.
- Ao receber uma nova requisição para este projeto, atuar como Arquiteto de Software e usar este `states.md` como ESTADO ATUAL.
- A análise deve respeitar a stack, a arquitetura, as fronteiras de banco e os contratos de dados descritos neste arquivo.
- A resposta de planejamento deve retornar somente o bloco `## Tarefas Pendentes`, formatado em Markdown.
- O bloco deve decompor a requisição em tarefas sequenciais, lógicas e granulares, especificando arquivos exatos, variáveis, tipagens e validações que deverão ser alterados ou criados.
- É proibido incluir saudações, conclusões, explicações fora dos bullets ou reescrever outras seções durante a resposta de planejamento.

## Tarefas Pendentes
- Nenhuma pendência registrada no momento.
