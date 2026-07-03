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
- `backup-dashboard-db.ps1`
  Executa backup `COPY_ONLY` do banco próprio `DASHBOARDS`/`DASHBOARDS_DEV`, valida com `RESTORE VERIFYONLY` e aplica retenção local.
- `install-dashboard-backup-task.ps1`
  Registra uma tarefa diária no Agendador do Windows chamando `backup-dashboard-db.ps1`; não roda produção nem reinicia serviços.

### Pré-requisitos

- API dev local disponível em `http://localhost:5011`
- SQL Server acessível com as credenciais de `backend/.env`
- `backend/.env` preenchido com `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`
- Java no `PATH` e driver `mssql-jdbc` disponível no repositório Maven local
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

Backup manual do banco próprio:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backup-dashboard-db.ps1 -EnvFile .\.env -BackupDirectory C:\Dashboards\backups\sqlserver -RetentionDays 14
```

Instalação da rotina diária no Agendador do Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\install-dashboard-backup-task.ps1 -EnvFile .\.env -BackupDirectory C:\Dashboards\backups\sqlserver -At 02:15 -RetentionDays 14
```

O backup usa apenas `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DASHBOARDS_BACKUP_DIR` e `DASHBOARDS_BACKUP_RETENTION_DAYS` do ambiente. O script bloqueia bancos fora de `DASHBOARDS`/`DASHBOARDS_DEV`.

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
