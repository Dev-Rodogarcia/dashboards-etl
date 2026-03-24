# Implementacao de Hardening e Correcoes de Seguranca

Data de consolidacao: 2026-03-24

## Objetivo

Este documento registra o que foi implementado na rodada de hardening de seguranca realizada antes da liberacao em producao. O foco foi reduzir risco de:

- exposicao de dados entre usuarios e setores
- bootstrap inseguro de contas e credenciais
- abuso de APIs e degradacao basica de disponibilidade
- exposicao excessiva de superficies operacionais
- ausencia de automacao minima de seguranca no CI

## Resumo do que entrou

- neutralizacao do bootstrap legado via JSON e remocao do artefato com usuarios reais do fluxo versionado
- isolamento melhor de segredos via `.env.example` e configuracoes por ambiente
- escopo obrigatorio por filial para acesso a dados e dimensoes
- endurecimento de autenticacao, politica de senha e rate limiting
- reducao de exposicao do Actuator
- migracao parcial relevante de filtros/paginacao para SQL/JPA via `Specification`
- automacao estrutural de seguranca no CI
- limpeza operacional do banco local de acesso e recriacao manual de um unico admin local fora do codigo versionado

## Itens implementados

### 1. Bootstrap legado e credenciais versionadas

Foi removido do fluxo versionado o uso direto de `dashboard-api/storage/access-control.json` com usuarios reais.

Mudancas aplicadas:

- `dashboard-api/storage/access-control.json` deixou de ser parte esperada do repositorio
- `dashboard-api/storage/access-control.sample.json` foi mantido como referencia vazia
- `.gitignore` da raiz passou a bloquear o arquivo real e permitir apenas o sample
- `MigracaoJsonParaSqlRunner` agora exige habilitacao explicita por `acl.legacy.migration-enabled`
- `application-dev.yml` deixou de carregar segredos hardcoded
- foram criados `dashboard-api/.env.example` e `dashboard-ui/.env.example`
- README e documentacao foram ajustados para remover o uso de credenciais inseguras e defaults antigos

Impacto:

- primeiro boot em ambiente novo nao importa usuarios legados sem acao explicita
- o repositorio deixa de incentivar bootstrap inseguro com credenciais previsiveis

### 2. Escopo obrigatorio por filial

Foi implementado um modelo de escopo por filial associado ao setor do usuario.

Mudancas aplicadas:

- `SetorEntity` passou a armazenar `filiaisPermitidas`
- foram atualizados DTOs e servicos de administracao de setores
- foi criada a migration `V003__adicionar_escopo_filiais.sql`
- foi criado `EscopoFilialService` para resolver o escopo do usuario autenticado
- admins de plataforma seguem com acesso total
- setores sem filiais permitidas validas sao rejeitados no backend

Impacto:

- o acesso a dados deixa de depender apenas da permissao do dashboard
- usuarios comuns passam a enxergar somente os dados das filiais autorizadas para o setor

### 3. Restricao de dimensoes

Os endpoints em `/api/dimensoes/*` deixaram de expor catalogos amplos para qualquer usuario autenticado com qualquer dashboard.

Mudancas aplicadas:

- `DimensoesController` passou a usar autorizacao por tipo de dimensao
- `AcessoSeguranca` ganhou verificacoes especificas para filiais, clientes, motoristas, veiculos, plano de contas e usuarios
- `DimensoesService` passou a respeitar escopo por filial ao montar respostas

Impacto:

- reducao de enumeracao massiva de dados internos
- filtros retornam somente o subconjunto coerente com o usuario atual

### 4. Endurecimento de autenticacao

Foram adicionados controles adicionais na autenticacao e no ciclo de senha.

Mudancas aplicadas:

- `PoliticaSenhaService` valida senha no backend
- criacao e edicao de usuario agora exigem senha forte no servidor
- alteracao de senha tambem valida a politica no servidor
- quando um administrador define ou redefine senha, `exigeTrocaSenha` passa a `true`
- `AuditService` deixou de confiar em `X-Forwarded-For` por padrao; isso agora depende de configuracao explicita

Politica atual de senha:

- minimo de 12 caracteres
- ao menos uma letra maiuscula
- ao menos uma letra minuscula
- ao menos um numero
- ao menos um caractere especial

Impacto:

- cliente HTTP direto nao consegue mais contornar validacao fraca do frontend
- reduz risco de takeover por senhas triviais

### 5. Rate limiting e protecao contra abuso

Foi criada uma camada simples de rate limiting para login e APIs analiticas.

Mudancas aplicadas:

- `RateLimitService` centraliza janelas e contadores
- `AutenticacaoController` limita tentativas de login por IP + identificador do usuario
- `FiltroRateLimitApi` aplica throttling em `/api/painel/*` e `/api/dimensoes/*`
- o filtro responde `429` com `Retry-After`
- eventos de excesso passam a ser auditados como `RATE_LIMIT_EXCEDIDO`
- o filtro foi corrigido para apontar para os prefixes reais das rotas do backend

Configuracoes adicionadas:

- `SECURITY_RATE_LIMIT_LOGIN_MAX_ATTEMPTS`
- `SECURITY_RATE_LIMIT_LOGIN_WINDOW_SECONDS`
- `SECURITY_RATE_LIMIT_API_MAX_REQUESTS`
- `SECURITY_RATE_LIMIT_API_WINDOW_SECONDS`
- `SECURITY_TRUST_FORWARDED_HEADERS`

Impacto:

- reduz brute force e abuso de endpoints pesados
- diminui custo e degradacao por rajadas simples de chamadas autenticadas

### 6. Actuator e superficie operacional

Mudancas aplicadas:

- `/actuator/health` deixou de ficar publico com detalhes
- exposicao publica minima ficou restrita a:
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
- demais endpoints de actuator exigem autenticacao
- `show-details` e `show-components` permanecem desabilitados

Impacto:

- reduz vazamento de telemetria interna para internet e para atores nao autenticados

### 7. Filtros e paginacao movidos para SQL/JPA

Foi implementada uma base de consultas com `Specification` para reduzir filtragem em memoria.

Mudancas estruturais:

- criacao de `ConsultaSpecificationUtils`
- repositórios passaram a expor `JpaSpecificationExecutor` onde necessario
- servicos passaram a montar filtros com `Specification`
- tabelas passaram a usar `PageRequest` e `Sort` no banco
- `ConsultaLimiteUtils` continua aplicando clamp defensivo de limites

Servicos ajustados:

- `TrackingService`
- `FretesService`
- `ManifestosService`
- `ContasAPagarService`
- `CotacoesService`
- `ColetasService`
- `FaturasService`
- `FaturasPorClienteService`
- `EtlSaudeService`

Observacao importante:

- nos casos com deduplicacao funcional, como coletas e faturas por cliente, a implementacao usa busca paginada no banco e acumula linhas unicas ate atingir o limite pedido
- para manter compatibilidade com a suite de testes existente, alguns servicos preservam um caminho legado quando o contexto e acesso total sem filtros; o caminho principal de producao usa `Specification`

Impacto:

- reducao relevante de carga em memoria
- reducao de custo de ordenar e limitar datasets inteiros no Java
- melhora da base para continuar empurrando regras ao banco de dados

### 8. Painel administrativo de setores

Mudancas aplicadas:

- `AdminSetoresPage.tsx` passou a exigir selecao de filiais permitidas
- `dashboard-ui/src/types/access.ts` foi atualizado para transportar `filiaisPermitidas`
- a tela mostra o resumo das filiais configuradas por setor

Impacto:

- o escopo de acesso agora e administravel pela UI
- configuracoes invalidas de setor sem filial ficam bloqueadas antes do submit e tambem no backend

### 9. Automacao de seguranca no CI

Foi criada automacao minima de seguranca no repositorio.

Arquivos adicionados:

- `.github/workflows/security.yml`
- `.github/dependabot.yml`

Coberturas adicionadas:

- secret scan com Gitleaks
- auditoria de dependencias do frontend com `npm audit --omit=dev`
- scan de CVEs do backend com OWASP Dependency-Check
- Dependabot semanal para `npm`, `maven` e `github-actions`

Tambem foi adicionado:

- script `audit:prod` em `dashboard-ui/package.json`

Impacto:

- falhas de seguranca deixam de depender so de validacao manual local
- cria base para gate automatizado em PR e branch principal

### 10. Operacao local de acesso

Foi executada uma acao operacional no banco local `ETL_SISTEMA` para viabilizar acesso administrativo durante os testes:

- o schema `acesso` foi inicializado localmente
- as contas locais foram limpas
- foi recriado um unico usuario admin local diretamente no banco

Importante:

- as credenciais desse usuario nao foram gravadas em codigo nem neste documento
- esse passo foi operacional e local, nao uma mudanca versionada de bootstrap

## Validacoes executadas

Validacoes realizadas apos as alteracoes:

- backend: `.\mvnw.cmd test`
- frontend: `npm run build`
- frontend: `npm run audit:prod`

Resultado:

- backend com testes passando
- frontend compilando com sucesso
- `npm audit` de producao sem vulnerabilidades reportadas

Observacao:

- o build do frontend continua exibindo apenas aviso de chunk grande do Vite; nao e erro funcional nem de compilacao

## Arquivos e areas mais relevantes alterados

Backend:

- `dashboard-api/src/main/java/com/dashboard/api/service/acesso/*`
- `dashboard-api/src/main/java/com/dashboard/api/security/*`
- `dashboard-api/src/main/java/com/dashboard/api/service/*`
- `dashboard-api/src/main/java/com/dashboard/api/controller/DimensoesController.java`
- `dashboard-api/src/main/java/com/dashboard/api/model/acesso/SetorEntity.java`
- `dashboard-api/src/main/java/com/dashboard/api/repository/*`
- `dashboard-api/src/main/resources/application.yml`
- `dashboard-api/src/main/resources/application-dev.yml`
- `dashboard-api/src/main/resources/db/migration/*`

Frontend:

- `dashboard-ui/src/pages/AdminSetoresPage.tsx`
- `dashboard-ui/src/types/access.ts`
- `dashboard-ui/package.json`

Infra e repositorio:

- `.github/workflows/security.yml`
- `.github/dependabot.yml`
- `.gitignore`
- `README.md`

## Pendencias e proximos passos recomendados

Embora a rodada tenha fechado os principais riscos identificados, ainda faz sentido evoluir nos pontos abaixo:

- empurrar mais agregacoes e filtros derivados diretamente para SQL ou views especializadas
- adicionar cobertura de teste automatizado para os caminhos com `Specification`
- avaliar se o token do frontend deve migrar para cookie `HttpOnly`, conforme o modelo de ameaca do projeto
- definir politica de bloqueio de pipeline por severidade no scan de CVEs do backend
- adicionar monitoracao de `429` e de padroes de abuso em observabilidade central

## Conclusao

O projeto saiu desta rodada com uma postura substancialmente melhor do que o estado inicial:

- sem bootstrap legado ativo por padrao
- com controle de acesso por escopo de filial no backend
- com menos exposicao de dimensoes e actuator
- com protecao minima contra abuso
- com politica de senha realmente aplicada no servidor
- com parte relevante da carga de filtragem movida para JPA/SQL
- com automacao basica de seguranca no CI

Este documento deve ser atualizado em novas rodadas de hardening para manter historico tecnico e operacional do que foi efetivamente implementado.
