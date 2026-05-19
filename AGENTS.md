# Regras Para IAs Neste Projeto

Este projeto deve ser operado por IA somente em modo DEV local.

- Nao execute `iniciar-prod.bat`.
- Nao reinicie, pare ou libere portas de producao (`5010` e `5173`).
- Nao suba servicos apontando para os dominios publicos `analytics.rodogarcia.com.br` ou `api-analytics.rodogarcia.com.br`.
- Use apenas `iniciar-dev.bat`, backend local `http://127.0.0.1:5011` e frontend local `http://127.0.0.1:5174`.
- Para smoke tests, use somente a API dev local. Producao deve ficar para operador humano fora do fluxo da IA.

Se uma tarefa exigir deploy/producao, a IA deve parar e avisar que o ambiente permitido para ela e apenas DEV local.
