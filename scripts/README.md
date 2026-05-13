# Scripts

## Validação automática dos dashboards

Este diretório contém a automação que compara os KPIs do SQL Server com os valores consumidos pela UI via API.

### Arquivos

- `validate-dashboard-consistency.mjs`
  Runner principal. Executa as queries no SQL Server, chama os endpoints `/api/painel/*`, compara as métricas e gera os relatórios.
- `validate-gestao-vista-xlsx-vs-dashboard.mjs`
  Validação específica de Gestão à Vista. Usa o XLSX de divergências como fonte da verdade, chama a API local que alimenta o dashboard e gera relatório `.md`/`.json` com OK/ERRO por métrica.
- `dashboard-validation/entities.mjs`
  Catálogo das entidades validadas, queries SQL-resumo e mapeamento entre aliases do banco e campos da API/UI.

### Pré-requisitos

- API dev local disponível em `http://localhost:5011`
- SQL Server acessível com as credenciais de `backend/.env`
- `backend/.env` preenchido com `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`
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
node scripts/validate-dashboard-consistency.mjs --apiBaseUrl=http://localhost:5011 --apiUserEmail=desenvolvedor@rodogarcia.com.br
```

Validação Gestão à Vista a partir do XLSX:

```powershell
node scripts/validate-gestao-vista-xlsx-vs-dashboard.mjs --dataInicio=2026-03-01 --dataFim=2026-03-31
```

Com XLSX/API explícitos:

```powershell
node scripts/validate-gestao-vista-xlsx-vs-dashboard.mjs --xlsx="Análise - Divergências - Indicadores Projeto Gestão a Vista Operacional.xlsx" --apiBaseUrl=http://localhost:5011
```

Runner para Agendador do Windows ou CI self-hosted:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-gestao-vista-daily-validation.ps1 -DataInicio 2026-03-01 -DataFim 2026-03-31 -ApiBaseUrl http://localhost:5011
```

### Saída

O script gera dois arquivos em `reports/`:

- `validacao-dashboard-<dataInicio>_<dataFim>.md`
- `validacao-dashboard-<dataInicio>_<dataFim>.json`
- `validacao-gestao-vista-xlsx-dashboard-<dataInicio>_<dataFim>.md`
- `validacao-gestao-vista-xlsx-dashboard-<dataInicio>_<dataFim>.json`

O `.md` é o relatório para leitura rápida.
O `.json` guarda os dados brutos da comparação, incluindo a reconciliação detalhada por chave quando a API expõe linha a linha.

Para Gestão à Vista, a validação oficial é zero divergência: contagens precisam bater com diferença `0`, moeda precisa bater no centavo e percentuais usam o mesmo arredondamento visual do dashboard. O script sai com código diferente de zero quando qualquer métrica comparável fica `ERRO`.

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

- A comparação geral usa tolerância por tipo de métrica, por exemplo `%` e valores decimais; a validação XLSX vs Dashboard de Gestão à Vista usa tolerância zero nas métricas comparáveis.
- O script trata `NULL` versus `0` para evitar falso positivo em agregações vazias.
- A coleta do "frontend" é feita pela mesma API consumida pela UI, não por scraping.
