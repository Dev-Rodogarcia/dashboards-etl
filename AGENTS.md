# Regras Para IAs Neste Projeto

Este projeto deve ser operado por IA somente em modo DEV local.

- Nao execute `iniciar-prod.bat`.
- Nao reinicie, pare ou libere portas de producao (`5010` e `5173`).
- Nao suba servicos apontando para os dominios publicos `analytics.rodogarcia.com.br` ou `api-analytics.rodogarcia.com.br`.
- Use apenas `iniciar-dev.bat`, backend local `http://127.0.0.1:5011` e frontend local `http://127.0.0.1:5174`.
- Para smoke tests, use somente a API dev local. Producao deve ficar para operador humano fora do fluxo da IA.
- Cada macaco no seu galho: migrations do projeto `dashboards` devem alterar apenas objetos de propriedade do Dashboard. Nao crie migrations aqui para sincronizar, alterar ou exigir estrutura interna do projeto `etl-extracao-dados`.
- Mudancas de tabelas, views, indices ou regras materializadas do ETL devem ser feitas no repositorio `etl-extracao-dados`; o Dashboard apenas consome o contrato publicado pela view.
- Sempre que criar ou alterar uma migration deste projeto, atualize tambem o script base/canonico correspondente em `databases/DASHBOARDS` e, quando aplicavel, a copia em `backend/src/main/resources/db/migration`, para uma recriacao limpa nascer no schema atual.
- Encoding e mojibake: preserve acentos, simbolos e aliases exatamente como publicados pelo banco/API. Nao aceite texto corrompido por encoding, como UTF-8 lido como ANSI/Windows-1252; se encontrar alias, migration, view, CSV, seed, fixture, doc ou teste com caracteres estranhos no lugar de acentos/simbolos, corrija a origem e valide usando UTF-8 antes de seguir.

Se uma tarefa exigir deploy/producao, a IA deve parar e avisar que o ambiente permitido para ela e apenas DEV local.
