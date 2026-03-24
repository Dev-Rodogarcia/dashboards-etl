Lacuna 1: A Vulnerabilidade do Webhook (Segurança Server-to-Server)
Nós definimos que o seu ETL (Java) vai disparar uma requisição POST para a API (Spring Boot) avisando para limpar o cache.

O Risco: Esse endpoint (/api/interno/webhook/etl-concluido) não pode ser protegido pelo Active Directory (LDAP), pois o ETL é um robô, não um funcionário humano com senha de rede. Se deixarmos essa rota pública, qualquer pessoa na intranet com o Postman pode criar um script que chama essa URL mil vezes por segundo, limpando o cache e derrubando o seu SQL Server por tabela (Ataque de Negação de Serviço - DoS interno).

A Solução: Precisamos definir um Filtro de API Key. O ETL enviará um Header fixo (ex: X-API-KEY: <defina-uma-chave-forte-e-unica>). O Spring Security precisa ser configurado para ignorar o LDAP nesta rota específica e validar apenas essa chave.

Lacuna 2: Observabilidade e Auditoria (O "Caixa Preta")
O seu sistema vai rodar sozinho num servidor. Se o Active Directory cair ou a View do SQL Server corromper, o Java vai capturar o erro (nosso ManipuladorGlobalExcecao) e devolver o erro 503 para o React. Mas e você, como desenvolvedor, como vai descobrir o que aconteceu?

O Risco: Ficar cego em produção. Entrar no servidor e não saber o motivo da falha.

A Solução: Precisamos documentar a integração do Spring Boot Actuator (para expor um endpoint /health que monitora se o banco de dados e o LDAP estão vivos) e definir a estrutura de Logs (Logback), para gravar arquivos físicos no servidor (/logs/dashboard-api.log) registrando cada tentativa de falha de login e tempo de resposta lento nas queries.

Lacuna 3: O Gargalo do CORS (Cross-Origin Resource Sharing)
Esse é o erro que mais trava iniciantes ao integrar React com Spring Boot. O seu React estará rodando em uma porta/IP (ex: http://intranet-ui:3000) e o Java em outra (ex: http://intranet-api:8080).

O Risco: Por padrão de segurança, os navegadores modernos (Chrome, Edge) bloqueiam qualquer requisição JavaScript (Axios) que tente ler dados de um domínio diferente. O Spring Boot rejeitará as requisições do seu dashboard, mesmo que o usuário esteja logado.

A Solução: Documentar estritamente a classe CorsConfig.java no backend para aceitar o IP exato onde o frontend estará hospedado, e permitir o tráfego dos Headers de Autorização (o seu JWT).

Problema 4: A Explosão de Memória (Falta de Limite de Payload)
Nós construímos filtros de data para os gráficos. O que impede o usuário do Financeiro de selecionar no calendário o período de "01/01/2015 a 31/12/2026"?

O Risco: O SQL Server vai processar isso, mas o retorno será de milhões de linhas. O Java tentará alocar tudo isso na memória RAM para converter em um JSON gigante. O resultado é um OutOfMemoryError que derruba a API inteira, ou o navegador do usuário simplesmente congela ao tentar renderizar o ECharts.

A Solução: O Java deve ter uma trava rígida (Validação de Domínio) na camada de Service. Se a diferença entre a dataInicio e dataFim for maior que, digamos, 90 dias, o backend recusa a requisição com um erro 400 Bad Request informando que o período é muito longo, forçando o usuário a refinar a busca.

Problema 5: O Token "Imortal" (Ciclo de Vida do JWT)
Nós optamos por usar o Active Directory para autenticação e gerar um JWT (que é stateless, ou seja, não fica salvo no banco).

O Risco: Se você configurar esse JWT para durar 24 horas e um funcionário for desligado da empresa ao meio-dia (tendo seu acesso cortado na rede), o dashboard dele continuará funcionando perfeitamente até o dia seguinte. Por quê? Porque o Java confia na assinatura do JWT e não bate no servidor AD a cada clique.

A Solução: Tokens de vida curta (ex: 15 a 30 minutos). Se você quiser uma experiência fluida, precisará implementar um mecanismo de Refresh Token, ou assumir que o usuário precisará logar novamente após ficar inativo.

Problema 6: Esgotamento de Conexões (Timeouts Ocultos)
O Spring Boot usa o HikariCP para gerenciar um pool de conexões com o SQL Server (por padrão, 10 conexões simultâneas).

O Risco: Suponha que o seu ETL esteja rodando e cause um lock (bloqueio) temporário em uma tabela. Se 10 usuários abrirem o dashboard nesse exato momento, as 10 conexões do Java ficarão "esperando" o SQL Server liberar a tabela. O pool esgota. O 11º usuário que tentar logar no sistema vai receber um erro de indisponibilidade, derrubando o sistema inteiro por um gargalo no banco.

A Solução: Configurar limites estritos de query-timeout e connection-timeout. Se o banco não responder em 3 segundos, o Java corta a conexão abortando a query e devolve o nosso já planejado erro 503 para o frontend. É melhor um gráfico falhar do que a API inteira travar.
