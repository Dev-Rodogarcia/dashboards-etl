# Setup, Ambientes e Validacao

## Requisitos locais

- Java 17 ou superior
- Node.js com npm
- acesso ao SQL Server usado pela API
- arquivo `.env` na raiz de `dashboards-etl`

## Configuracao de ambiente

### Arquivo central

Arquivo:

- `.env`

Variaveis minimas:

```env
API_BASE_URL=http://localhost:5010
VITE_API_BASE_URL=http://localhost:5010
SERVER_ADDRESS=127.0.0.1
DB_URL=jdbc:sqlserver://HOST:1433;databaseName=ETL_SISTEMA;encrypt=true;trustServerCertificate=true
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=segredo-forte
API_KEY=chave-interna
AUTH_SESSION_EXPIRACAO_HORAS=24
ACESSO_USUARIO_SUPREMO_EMAIL=admin-seguro@empresa.com
ACESSO_USUARIO_SUPREMO_SENHA_INICIAL=senha-forte-fora-do-codigo
ACESSO_USUARIO_SUPREMO_NOME=Administrador Supremo
ACESSO_USUARIO_SUPREMO_PAPEL=desenvolvedor
ACESSO_USUARIO_SUPREMO_NIVEL=1000
VITE_ACESSO_USUARIO_SUPREMO_PAPEL=desenvolvedor
```

Configuracoes importantes no `application.yml`:

- porta `5010`
- timeout JPA `30000`
- periodo timezone `America/Sao_Paulo`
- JWT `15 minutos`
- bind local da API por padrao em `127.0.0.1`

## Producao com Cloudflare Tunnel

Na VM, use as variaveis abaixo junto das credenciais reais:

```env
SERVER_ADDRESS=127.0.0.1
SECURITY_TRUST_FORWARDED_HEADERS=true
AUTH_REFRESH_COOKIE_SECURE=true
AUTH_REFRESH_COOKIE_SAME_SITE=Lax
AUTH_SESSION_EXPIRACAO_HORAS=24
ACESSO_USUARIO_SUPREMO_EMAIL=admin-seguro@empresa.com
ACESSO_USUARIO_SUPREMO_SENHA_INICIAL=senha-forte-fora-do-codigo
ACESSO_USUARIO_SUPREMO_NOME=Administrador Supremo
ACESSO_USUARIO_SUPREMO_PAPEL=desenvolvedor
ACESSO_USUARIO_SUPREMO_NIVEL=1000
VITE_ACESSO_USUARIO_SUPREMO_PAPEL=desenvolvedor
CORS_ORIGENS_PERMITIDAS=https://analytics.rodogarcia.com.br
```

Permissoes minimas esperadas para o usuario SQL da API no schema `acesso`:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA :: acesso TO usuario_etl;
```

Se a tabela `acesso.refresh_tokens` ainda nao existir e voce optar por deixar a aplicacao cria-la no startup, tambem sera necessario `GRANT CREATE TABLE TO usuario_etl;`. A opcao mais controlada e aplicar as migrations no banco antes do deploy.

Para permitir varios usuarios novos sem `chave_legado`, aplique a migration `V009__corrigir_unique_chave_legado_usuarios.sql` no banco de producao antes de validar o cadastro de usuarios. Ela troca a restricao `UNIQUE` comum por um indice unico filtrado que ignora `NULL`.

## Como subir localmente

### Opcao 1: script do monorepo

```powershell
.\iniciar-dev.bat
```

Resultado esperado:

- API em `http://localhost:5010`
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

## Observacao importante sobre Java no Windows

Ja ocorreu de `java -version` apontar para Java 8 enquanto o Maven wrapper usava um JDK moderno.

Sinais de problema:

- `java -jar` falha com `UnsupportedClassVersionError`;
- `.\mvnw.cmd -v` mostra um JDK mais novo que `java -version`.

Acao recomendada:

- preferir `.\mvnw.cmd spring-boot:run`; ou
- ajustar `JAVA_HOME` e `PATH` antes de rodar o jar diretamente.

## Testes de rotina

Backend:

```powershell
cd .\dashboard-api
.\mvnw.cmd test
```

Frontend:

```powershell
cd .\dashboard-ui
npm test -- --run
npx tsc --noEmit
npm run lint
npm run build
npm run check:encoding
```

## Validacao automatica do BI

Script principal:

- `scripts/validate-dashboard-consistency.mjs`

Pre requisitos:

- API local respondendo em `5010`
- SQL Server acessivel com as credenciais do `.env`
- ao menos um usuario ativo em `acesso.usuarios`

Execucao simples:

```powershell
node scripts/validate-dashboard-consistency.mjs
```

Execucao com periodo explicito:

```powershell
node scripts/validate-dashboard-consistency.mjs --apiBaseUrl=http://localhost:5010 --dataInicio=2026-02-24 --dataFim=2026-03-26
```

Saida:

- `reports/validacao-dashboard-<inicio>_<fim>.md`
- `reports/validacao-dashboard-<inicio>_<fim>.json`

## Bateria recomendada antes de fechar bug de filtro

Rodar pelo menos:

- `7d`
- `15d`
- `30d`
- `60d`
- `90d`
- `180d`

Motivo:

- garante que o problema nao esta escondido so na fronteira de periodo;
- detecta regressao em dashboards com `DATETIMEOFFSET`;
- diferencia bug de codigo de oscilacao de dados ao vivo no ETL.

## Gate minimo para considerar uma mudanca pronta

- testes backend passando;
- testes frontend passando;
- TypeScript limpo;
- lint limpo;
- build de producao passando;
- validacao BI em `100%` para os periodos relevantes;
- documentacao atualizada se houver mudanca de contrato ou operacao.
