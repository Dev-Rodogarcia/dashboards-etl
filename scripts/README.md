# Scripts

## Validação automática dos dashboards

Este diretório contém a automação que compara os KPIs do SQL Server com os valores consumidos pela UI via API.

### Arquivos

- `validate-dashboard-consistency.mjs`
  Runner principal. Executa as queries no SQL Server, chama os endpoints `/api/painel/*`, compara as métricas e gera os relatórios.
- `dashboard-validation/entities.mjs`
  Catálogo das entidades validadas, queries SQL-resumo e mapeamento entre aliases do banco e campos da API/UI.

### Pré-requisitos

- API local disponível em `http://localhost:5010`
- SQL Server acessível com as credenciais de `dashboard-api/.env`
- `dashboard-api/.env` preenchido com `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`
- Pelo menos um usuário ativo em `acesso.usuarios`

### Como rodar

Na raiz do repositório:

```powershell
node scripts/validate-dashboard-consistency.mjs
```

Com período explícito:

```powershell
node scripts/validate-dashboard-consistency.mjs --dataInicio=2026-03-01 --dataFim=2026-03-31
```

Com overrides opcionais:

```powershell
node scripts/validate-dashboard-consistency.mjs --apiBaseUrl=http://localhost:5010 --apiUserEmail=desenvolvedor@rodogarcia.com.br
```

### Saída

O script gera dois arquivos em `reports/`:

- `validacao-dashboard-<dataInicio>_<dataFim>.md`
- `validacao-dashboard-<dataInicio>_<dataFim>.json`

O `.md` é o relatório para leitura rápida.
O `.json` guarda os dados brutos da comparação.

### O que é validado

- Coletas
- Fretes
- Faturas
- Cotações
- Contas a Pagar
- Localização de Cargas
- Manifestos
- Faturas por Cliente
- ETL Saúde
- Executivo

### Observações

- A comparação usa tolerância por tipo de métrica, por exemplo `%` e valores decimais.
- O script trata `NULL` versus `0` para evitar falso positivo em agregações vazias.
- A coleta do "frontend" é feita pela mesma API consumida pela UI, não por scraping.
