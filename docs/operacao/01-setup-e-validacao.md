# Setup, Ambientes e Validacao

## Requisitos locais

- Java 17 ou superior
- Node.js com npm
- acesso ao SQL Server usado pela API
- arquivo `dashboard-api/.env`
- arquivo `dashboard-ui/.env.development`

## Configuracao de ambiente

### Backend

Arquivo:

- `dashboard-api/.env`

Variaveis minimas:

```env
DB_URL=jdbc:sqlserver://HOST:1433;databaseName=ETL_SISTEMA;encrypt=true;trustServerCertificate=true
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=segredo-forte
API_KEY=chave-interna
```

Configuracoes importantes no `application.yml`:

- porta `5010`
- timeout JPA `30000`
- periodo timezone `America/Sao_Paulo`
- JWT `15 minutos`

### Frontend

Arquivo:

- `dashboard-ui/.env.development`

Variavel minima:

```env
VITE_API_BASE_URL=http://localhost:5010
```

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
