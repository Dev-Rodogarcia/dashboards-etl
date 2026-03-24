# Dashboards ETL

Monorepo com backend Spring Boot e frontend React para dashboards operacionais, financeiros e de monitoramento do ETL. O projeto centraliza autenticacao, autorizacao por setor, consultas em SQL Server e filtros compartilhados por URL.

## Escopo atual

- 10 areas protegidas no frontend: `Coletas`, `Manifestos`, `Fretes`, `Tracking`, `Faturas`, `Faturas por Cliente`, `Contas a Pagar`, `Cotacoes`, `Executivo` e `ETL Saude`
- area administrativa para gestao de `setores` e `usuarios`
- backend com JWT, ACL local em JSON e endpoints por dominio em `/api/painel/*`
- dimensoes compartilhadas em `/api/dimensoes/*`
- healthchecks publicos minimos em `/actuator/health/liveness` e `/actuator/health/readiness`
- logs locais da API em `dashboard-api/logs/`

## Estrutura do repositorio

```text
dashboards-etl/
|-- dashboard-api/      # Spring Boot 3.2 / Java 17
|-- dashboard-ui/       # React 19 / TypeScript / Vite
|-- docs/               # documentacao funcional e tecnica
|-- public/             # ativos compartilhados
|-- .vscode/            # configuracoes compartilhadas do workspace
`-- iniciar-dev.bat     # sobe backend + frontend em paralelo no Windows
```

## Stack

### Backend

- Java 17
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- SQL Server
- JWT (`jjwt`)
- Spring Boot Actuator

### Frontend

- React 19
- TypeScript
- Vite
- React Router DOM
- TanStack Query
- Axios
- Apache ECharts
- Tailwind CSS v4

## Requisitos

- Java 17 ou superior
- Node.js com npm
- acesso a um SQL Server compativel com as views esperadas pelo backend

## Configuracao local

### Backend (`dashboard-api/.env`)

Use `dashboard-api/.env.example` como base.

```env
DB_URL=jdbc:sqlserver://HOST:1433;databaseName=ETL_SISTEMA;encrypt=true;trustServerCertificate=true
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=segredo-forte-unico-por-ambiente
API_KEY=chave-forte-unica-para-rotas-internas
```

Notas:

- `JWT_SECRET` e `API_KEY` devem ser definidos explicitamente em todos os ambientes.
- a migração legada JSON → SQL fica desabilitada por padrão e só deve ser habilitada de forma temporária via `ACL_LEGACY_MIGRATION_ENABLED=true`.
- não versione usuários reais, hashes de senha ou artefatos de bootstrap no deploy.

### Frontend (`dashboard-ui/.env.development`)

Use `dashboard-ui/.env.example` como base.

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Como executar

### Opcao 1: script do monorepo

```powershell
.\iniciar-dev.bat
```

O script abre duas janelas separadas e inicia:

- API em `http://localhost:8080`
- UI em `http://localhost:5173`

### Opcao 2: manual

Backend:

```powershell
cd .\dashboard-api
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd .\dashboard-ui
npm install
npm run dev
```

## Fluxo de acesso em desenvolvimento

- a tela de login vive em `/login`
- a UI usa `VITE_API_BASE_URL` e consome autenticacao em `/api/auth/*`
- novos usuários exigem senha explícita e política server-side
- não existe endpoint público para descoberta de contas

## Rotas principais do frontend

- `/coletas`
- `/manifestos`
- `/fretes`
- `/tracking`
- `/faturas`
- `/faturas-por-cliente`
- `/contas-a-pagar`
- `/cotacoes`
- `/executivo`
- `/etl-saude`
- `/admin/setores`
- `/admin/usuarios`

## Endpoints principais do backend

- `/api/auth/*`
- `/api/painel/*`
- `/api/dimensoes/*`
- `/api/admin/acesso/*`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

## Convencoes relevantes

- filtros compartilhados sao serializados na URL como `f.<chave>`
- o periodo maximo aceito por frontend e backend e de `90 dias`
- logs da API sao gravados em `dashboard-api/logs/dashboard-api.log`

## Correcao manual de encoding da ACL local

Se a area administrativa exibir textos quebrados no modulo de acesso, aplique o script:

- `dashboard-api/src/main/resources/db/migration/V004__corrigir_mojibake_acesso.sql`

Depois valide que nao restaram registros corrompidos em `setores`, `papeis` e `permissoes`:

```sql
SELECT 'setores.nome' AS alvo, COUNT(*) AS qtd
FROM acesso.setores
WHERE nome COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(195) + N'%'
   OR nome COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(194) + N'%'
UNION ALL
SELECT 'setores.descricao', COUNT(*)
FROM acesso.setores
WHERE descricao COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(195) + N'%'
   OR descricao COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(194) + N'%'
UNION ALL
SELECT 'papeis.descricao', COUNT(*)
FROM acesso.papeis
WHERE descricao COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(195) + N'%'
   OR descricao COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(194) + N'%'
UNION ALL
SELECT 'permissoes.nome', COUNT(*)
FROM acesso.permissoes
WHERE nome COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(195) + N'%'
   OR nome COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(194) + N'%'
UNION ALL
SELECT 'permissoes.descricao', COUNT(*)
FROM acesso.permissoes
WHERE descricao COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(195) + N'%'
   OR descricao COLLATE Latin1_General_100_BIN2 LIKE N'%' + NCHAR(194) + N'%';
```

## Documentacao de apoio

- [docs/relatorio-refatoracao-consolidada.md](docs/relatorio-refatoracao-consolidada.md)
- [docs/implementacao-hardening-seguranca.md](docs/implementacao-hardening-seguranca.md)
- [docs/relatorio-bi-catalogo-views.md](docs/relatorio-bi-catalogo-views.md)
- [docs/relatorio-bi-dashboards-logistica.md](docs/relatorio-bi-dashboards-logistica.md)
- [docs/backend.md](docs/backend.md)
- [docs/frontend.md](docs/frontend.md)
- [docs/GUIA-INTEGRACAO-SQL-DTOS-VIEWS.md](docs/GUIA-INTEGRACAO-SQL-DTOS-VIEWS.md)
- [docs/validacao-manual-entidades-dashboard.md](docs/validacao-manual-entidades-dashboard.md)

## Observacoes

- o `.gitignore` da raiz cobre artefatos gerados do monorepo; os `.gitignore` internos continuam valendo para cada app
- `dashboard-api/storage/access-control.json` deve permanecer fora do repositório; use apenas `dashboard-api/storage/access-control.sample.json` como referência
