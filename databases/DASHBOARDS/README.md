# DASHBOARDS Database

Fonte organizada dos scripts SQL do banco `DASHBOARDS`.

## Estrutura

- `migrations/`: migrations versionadas usadas pela API.
- `setup/`: scripts operacionais de criacao, migracao e sincronizacao.
- `validation/`: checks de schema, permissao e pos-migration.

## Ordem Recomendada

1. Aplicar as migrations em `migrations/` na ordem `V001` ate a versao desejada.
2. Para corrigir somente o escopo de filiais por usuario, aplicar:

```powershell
sqlcmd -S localhost,1433 -d DASHBOARDS -E -C -b -f 65001 -i databases\DASHBOARDS\migrations\V011__adicionar_escopo_filiais_usuario.sql
```

3. Validar o schema:

```powershell
sqlcmd -S localhost,1433 -d DASHBOARDS -U usuario_etl -P "<senha>" -C -b -f 65001 -i databases\DASHBOARDS\validation\001_validar_escopo_filiais_usuario.sql
```

## Observacoes

- O usuario da aplicacao pode nao ter permissao de `ALTER` ou `CREATE TABLE`. Nesse caso, aplique migrations com usuario `dbo` ou credencial administrativa.
- O alvo desta pasta e sempre `DASHBOARDS`.
- Use `-f 65001` ao aplicar migrations via `sqlcmd`; as views de dashboard possuem aliases acentuados e devem ser lidas como UTF-8.
- `TODAS` significa acesso total as filiais da empresa; `HERDAR_SETOR` usa o escopo do setor; `SELECIONADAS` usa `acesso.usuario_filiais_permitidas`.
