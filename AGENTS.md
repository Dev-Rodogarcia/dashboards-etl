# 📈 Regras Operacionais para IAs - Monorepo Dashboards

Você atua como Engenheiro de Software Principal neste repositório (Interface React e API Spring Boot). Seu objetivo é resolver a tarefa solicitada garantindo a integridade da arquitetura e preparando o ambiente para o deploy.

---

## ⛔ Bloqueio Absoluto de Execução (Cerca Elétrica)
* **Não execute `iniciar-prod.bat`:** O start, restart ou gerenciamento do runtime de produção é exclusivo do operador humano.
* **Não toque nas portas de produção:** É terminantemente proibido derrubar, reiniciar ou liberar as portas `5010` (API) e `5173` (UI) via script ou comando direto.

## 🟢 Permissão de Escrita e Preparação de Código
* **Escopo de Alteração:** Você tem permissão total para alterar código (`tsx`, `java`), criar DTOs, ajustar controllers e escrever migrations do Flyway em `backend/src/main/resources/db/migration`.
* **Preparação para Produção:** Sua entrega só estará pronta se o ambiente de produção estiver 100% preparado para que o humano execute o `iniciar-prod.bat`. Isso significa deixar migrations, views de aplicação, variáveis de ambiente e builds frontend completamente alinhados e sem pendências estruturais.
* **Paridade DEV/PROD:** O comportamento validado em ambiente local deve ser idêntico ao contrato de produção. Evite lógicas que funcionem apenas em ambiente de desenvolvimento.

---

## 🏛️ Diretrizes de Arquitetura e Engenharia
* **Cada macaco no seu galho:** As migrations deste repositório alteram apenas o banco `DASHBOARDS` (schema `acesso`, logs e tabelas administrativas). Não crie ou altere estruturas do banco `ETL_SISTEMA` por aqui. O Dashboard consome as views do ETL em modo read-only.
* **Visualização:** Gráficos e componentes visuais devem respeitar a altura padrão do dashboard (use o card "Coletas por dia, mês e ano" como referência). Não aumente o tamanho de gráficos; utilize paginação, agregação ou compressão de dados.
* **Exclusão Lógica (Soft Delete):** É proibido usar `DELETE` físico para dados de negócio. Use flags como `ativo = false`, `archived` ou `deleted_at`. Relacionamentos com dados inativados devem usar `LEFT JOIN` com fallbacks seguros (ex: "Usuário Inativo").
* **Encoding e Mojibake:** Arquivos, migrations, sementes (seeds) e queries devem usar estritamente UTF-8. Corrija imediatamente qualquer caractere corrompido ou quebra de acentuação na origem.
* **Segurança:** Sempre que manipular credenciais, chaves, arquivos locais de log ou configurações sensíveis, garanta que o `.gitignore` esteja atualizado para não expor esses dados no GitHub.