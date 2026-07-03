# 📈 Regras Operacionais para IAs - Monorepo Dashboards

Você atua como Engenheiro de Software Principal neste repositório (Interface React e API Spring Boot). Seu objetivo é resolver a tarefa solicitada garantindo a integridade da arquitetura e preparando o ambiente para o deploy.

---

## ⛔ Bloqueio Absoluto de Execução (Cerca Elétrica)
* **Não execute `iniciar-prod.bat`:** O start, restart ou gerenciamento do runtime de produção é exclusivo do operador humano.
* **Não toque nas portas de produção:** É terminantemente proibido derrubar, reiniciar ou liberar as portas `5010` (API) e `5173` (UI) via script ou comando direto.

### 🚫 REGRA DE OURO: ZERO HARDCODE DE DATABASE NO DASHBOARD
É EXPRESSAMENTE PROIBIDO "chumbar" (hardcode) o prefixo do banco de dados analítico (ex: `[ETL_SISTEMA].dbo...` ou `catalog="ETL_SISTEMA"`) em repositórios, entidades JPA ou queries nativas do código Java do Dashboard. 
- O backend deve ignorar onde os dados moram e ler os objetos pelo nome simples (ex: `dbo.vw_manifestos_powerbi`).
- O isolamento entre DEV e PROD é garantido pela camada de banco (Flyway).
- Se o Dashboard precisar enxergar um objeto do ETL, crie uma migration no Flyway do Dashboard utilizando `SYNONYM` (ex: `CREATE SYNONYM dbo.vw_x FOR ETL_SISTEMA.dbo.vw_x`). Nunca crie Views Wrappers estáticas que engessem o schema.

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

## 📐 Governança de KPIs e Tooltips
* **Sincronização obrigatória:** Toda alteração em um KPI deve ser tratada como uma mudança de contrato entre dados, API e interface. Isso inclui fórmula, numerador, denominador, unidade, arredondamento, status incluídos ou excluídos, deduplicação, dimensão de calendário, período de referência, título ou interpretação de negócio.
* **Dicionário central:** Ao modificar qualquer regra de KPI no SQL, ETL, Repository, Service, DTO, hook ou componente React, atualize obrigatoriamente a definição correspondente em `frontend/src/constants/kpiDictionary.ts` na mesma entrega. Os campos `titulo`, `descricao`, `calculo` e `observacao` devem continuar refletindo exatamente a regra vigente em linguagem de negócio.
* **Dicionário central de gráficos:** Ao modificar qualquer lógica de agrupamento, fonte de dados ou regra matemática de um Gráfico no Backend/SQL, atualize obrigatoriamente a definição correspondente em `frontend/src/constants/chartDictionary.ts` na mesma entrega. O mesmo princípio de Sem divergência silenciosa dos KPIs se aplica aos gráficos.
* **Componente de exibição:** Os cards cobertos pelo dicionário devem permanecer integrados ao `frontend/src/components/shared/TooltipKpi.tsx`. Se a mudança exigir novos campos, outra organização das informações, comportamento de hover/foco ou ajuste de layout, atualize também o `TooltipKpi.tsx`. Mudanças apenas na fórmula não devem duplicar textos dentro do componente; o conteúdo continua pertencendo ao dicionário.
* **Sem divergência silenciosa:** É proibido entregar uma mudança de cálculo mantendo tooltip desatualizado, ou alterar o texto do tooltip sem validar a regra real na camada de dados. A camada SQL/ETL é a fonte da verdade matemática; o `kpiDictionary.ts` é a fonte central da explicação apresentada ao usuário.
* **Checklist de validação:** Toda mudança de KPI deve revisar os cards que consomem a definição, executar TypeScript, lint e testes do frontend e confirmar que o wrapper não alterou o comportamento responsivo de CSS Grid ou Flexbox.

## Diretrizes de Sincronização de Estado (states.md)
1. Antes de iniciar a implementação de qualquer código, você DEVE ler o arquivo `states.md` para compreender o contexto arquitetural e as regras de negócio vigentes, garantindo que as novas implementações não quebrem o estado atual.
2. Leia a seção "Tarefas Pendentes" no `states.md` para entender o escopo exato do que precisa ser desenvolvido.
3. Após finalizar a escrita e modificação do código, você DEVE atualizar o arquivo `states.md`.
4. A atualização consiste em: remover a tarefa concluída da seção "Tarefas Pendentes" e atualizar as seções "Arquitetura e Padrões", "Fluxo de Dados" ou "Regras de Negócio Consolidadas" refletindo exatamente o novo estado do sistema.
5. NUNCA entregue ou finalize uma modificação de código sem antes reescrever e atualizar o `states.md` para refletir o presente.
