# 📈 Regras Operacionais para IAs - Monorepo Dashboards

Você atua como Engenheiro de Software Principal neste repositório (Interface React e API Spring Boot). Seu objetivo é resolver a tarefa solicitada garantindo a integridade da arquitetura e preparando o ambiente para o deploy.

---

## ⛔ Bloqueio Absoluto de Execução (Cerca Elétrica)
* **Não execute `iniciar-prod.bat`:** O start, restart ou gerenciamento do runtime de produção é exclusivo do operador humano.
* **Não toque nas portas de produção:** É terminantemente proibido derrubar, reiniciar ou liberar as portas `5010` (API) e `5173` (UI) via script ou comando direto.

## 🟢 Permissão de Escrita e Preparação de Código
* **Escopo de Alteração:** Você tem permissão total para alterar código (`tsx`, `java`), criar DTOs, ajustar controllers e escrever migrations do Flyway em `database/migrations`.
* **Preservação de Documentação Operacional:** É proibido apagar arquivos `README.md` e `AGENTS.md`. Quando necessário, apenas atualize seu conteúdo mantendo esses arquivos presentes no repositório.
* **Preparação para Produção:** Sua entrega só estará pronta se o ambiente de produção estiver 100% preparado para que o humano execute o `iniciar-prod.bat`. Isso significa deixar migrations, views de aplicação, variáveis de ambiente e builds frontend completamente alinhados e sem pendências estruturais.
* **Paridade DEV/PROD:** O comportamento validado em ambiente local deve ser idêntico ao contrato de produção. Evite lógicas que funcionem apenas em ambiente de desenvolvimento.
* **Paridade de Schema (Baseline Parity):** Toda alteração estrutural via Migration/Flyway (ex: arquivos em `database/migrations/`) DEVE obrigatoriamente ser refletida nos scripts base, seeds ou artefatos canônicos de criação correspondentes quando existirem. A recriação do banco de dados do zero deve produzir um schema idêntico ao banco atualizado via migrations. Esta regra não autoriza DDL/DML no `ETL_SISTEMA`.

---

## 🗄️ Topologia de Bancos de Dados e Fronteiras Arquiteturais
* **`ETL_SISTEMA` (`esl_cloud`):** Trate este banco como domínio exclusivo do pipeline de extração e como fonte de verdade analítica. O backend do Dashboard deve consumir views e tabelas deste banco estritamente em modo **READ-ONLY** (`SELECT`). Nenhuma migration, inicializador, rotina Java ou script deste monorepo pode aplicar DDL/DML neste banco. Criação de colunas computadas, chaves, índices, tabelas base, views operacionais (`dbo.vw_*_powerbi`) ou views analíticas deve acontecer exclusivamente via scripts no repositório `etl-extracao-dados`.
* **`DASHBOARDS`:** Trate este banco como produção exclusiva da aplicação web. Ele armazena somente estado interno do portal: ACL (papéis, permissões, usuários e setores), sessões, configurações e auditoria administrativa. A única fonte de verdade estrutural deste banco é o **Flyway** do monorepo em `database/migrations`. O Hibernate deve operar com `ddl-auto=none`; DDL em runtime pelo Java é terminantemente proibido.
* **`DASHBOARDS_DEV`:** Trate este banco como sandbox de desenvolvimento local, usado pelo profile `dev` e por `.env.development.local`. Ele existe para evitar acidentes e poluição de dados na produção. Mantenha ativo o validador de infraestrutura `DevDatabaseIsolationValidator`, que executa *fast-fail* e aborta o startup quando o ambiente de desenvolvimento tenta conectar a JDBC principal ao banco `DASHBOARDS` de produção.

---

## 🏛️ Diretrizes de Arquitetura e Engenharia
* **Cada macaco no seu galho:** As migrations deste repositório alteram apenas o banco `DASHBOARDS` (schema `acesso`, logs e tabelas administrativas). Não crie ou altere estruturas do banco `ETL_SISTEMA` por aqui. O Dashboard consome as views do ETL em modo read-only.
* **Push-down Computation:** É expressamente proibido carregar massas de dados para a memória da JVM (ex: `.findAll().stream().collect(...)`) para realizar agrupamentos, contagens, somas, divisões, rankings ou totalizações. Toda matemática de conjuntos, `COUNT`, `SUM`, `AVG`, `MIN`, `MAX` e `GROUP BY` deve ser delegada ao SQL Server via repositórios JDBC/queries nativas. O Java atua apenas como roteador, orquestrador leve e renderizador de DTOs.
* **SARGability Crítica:** É proibido usar funções no lado esquerdo das cláusulas `WHERE` em colunas indexadas, principalmente datas (ex: `YEAR(coluna)`, `MONTH(coluna)`, `TRY_CONVERT(coluna)`, `COALESCE(coluna, ...)`). Filtros temporais devem ser construídos por intervalos diretos e sargable: `coluna >= :inicio AND coluna < :fimExclusivo`.
* **Clean Code e Pacotes:** Respeite a arquitetura em camadas. O pacote `service` é exclusivo para classes com `@Service`; repositórios e gateways SQL ficam em `repository`, utilitários puros em `util`, configurações em `config`, políticas em `policy`, filtros em `filter`, listeners em `listener`, builders em `builder` e contratos/definitions em seus pacotes próprios. Cada macaco no seu galho.
* **Visualização:** Gráficos e componentes visuais devem respeitar a altura padrão do dashboard (use o card "Coletas por dia, mês e ano" como referência). Não aumente o tamanho de gráficos; utilize paginação, agregação ou compressão de dados.
* **Exclusão Lógica (Soft Delete):** É proibido usar `DELETE` físico para dados de negócio. Use flags como `ativo = false`, `archived` ou `deleted_at`. Relacionamentos com dados inativados devem usar `LEFT JOIN` com fallbacks seguros (ex: "Usuário Inativo").
* **Encoding e Mojibake:** Arquivos, migrations, sementes (seeds) e queries devem usar estritamente UTF-8. Corrija imediatamente qualquer caractere corrompido ou quebra de acentuação na origem.
* **Segurança:** Sempre que manipular credenciais, chaves, arquivos locais de log ou configurações sensíveis, garanta que o `.gitignore` esteja atualizado para não expor esses dados no GitHub.
