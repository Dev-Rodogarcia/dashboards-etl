Frente 1: Segurança do Webhook (API Key Server-to-Server)
O seu robô ETL precisa limpar o cache da API sem passar pelo Active Directory (LDAP). Criaremos um filtro exclusivo para ele.

Arquivo: src/main/java/br/com/empresa/dashboard/security/FiltroApiKey.java

Mecânica: Uma classe que estende OncePerRequestFilter. Ela intercepta apenas requisições para a rota /api/interno/**.

Regra de Negócio: Verifica se o cabeçalho HTTP X-API-KEY contém a chave exata definida no seu application.yml (ex: <defina-uma-chave-forte-e-unica>). Se a chave estiver errada ou ausente, devolve um erro HTTP 401 (Unauthorized).

Ajuste no SegurancaWebConfig.java: Você deve instruir o Spring Security a ignorar o filtro JWT e o LDAP especificamente para as rotas que começam com /api/interno/, delegando a segurança dessa rota exclusivamente ao FiltroApiKey.

Frente 2: Observabilidade e Auditoria (Caixa Preta)
Se o sistema falhar de madrugada, você não terá o console da sua IDE para ler os erros. O servidor precisa gerar rastros persistentes e expor sua saúde.

Dependência (pom.xml): Adicione o spring-boot-starter-actuator.

Configuração (application.yml): Habilite o endpoint /actuator/health. Isso permite que a infraestrutura da rede (ou um script simples) chame essa URL para saber se a sua API conseguiu conectar no SQL Server e no Active Directory. Se o status retornar "DOWN", o alerta é disparado.

Arquivo de Log: src/main/resources/logback-spring.xml.

Regra de Retenção: Configure um RollingFileAppender. O sistema deve gravar os logs em um arquivo físico (ex: /logs/dashboard-api.log). Quando o arquivo atingir 10MB, o Java cria um novo e compacta o antigo. Nunca deixe os logs crescerem infinitamente, ou o servidor ficará sem espaço em disco.

Frente 3: Configuração Estrita de CORS
O bloqueio de CORS impede que domínios diferentes conversem. A configuração deve ser cirúrgica, rejeitando origens desconhecidas.

Arquivo: src/main/java/br/com/empresa/dashboard/config/CorsGlobalConfig.java

Mecânica: Implemente a interface WebMvcConfigurer e sobrescreva o método addCorsMappings.

Regras Obrigatórias:

allowedOrigins: Defina o IP e a porta exatos de onde o frontend (React) será servido na intranet (ex: http://192.168.1.50). Não use *.

allowedMethods: Apenas GET, POST e OPTIONS. O dashboard não faz PUT ou DELETE.

allowedHeaders: Libere explicitamente o cabeçalho Authorization (onde vai o JWT) e Content-Type.

Frente 4: Trava de Memória (Validação de Domínio)
Não permita que o usuário tente buscar todo o histórico da empresa de uma vez, esgotando a memória RAM do Java.

Arquivo: src/main/java/br/com/empresa/dashboard/service/ValidadorFiltroService.java

Mecânica: Antes do FinanceiroService chamar o banco de dados, ele passa as datas por este validador.

Regra de Negócio: Calcule a diferença em dias entre a dataInicio e dataFim usando java.time.temporal.ChronoUnit.DAYS.between(). Se o limite configurado for de 90 dias e a requisição pedir 120 dias, lance uma exceção customizada PeriodoInvalidoException.

Saída: O ManipuladorGlobalExcecao captura essa falha e devolve um erro HTTP 400 (Bad Request) com a mensagem clara: "O período máximo de consulta é de 90 dias". O banco de dados nem chega a ser acionado.

Frente 5: Ciclo de Vida Curto do Token JWT
Para garantir que o bloqueio de um funcionário no Active Directory reflita rapidamente no dashboard, o token não pode ter vida longa.

Arquivo: src/main/java/br/com/empresa/dashboard/security/GeradorTokenJwt.java

Mecânica: Na geração do JWT (payload), defina o tempo de expiração (setExpiration) para no máximo 30 minutos a partir da emissão.

Consequência Arquitetural: Após 30 minutos de uso, a próxima requisição que o React fizer vai receber um erro HTTP 401. O frontend deverá forçar o usuário de volta para a tela de login. Isso evita a complexidade de gerenciar Refresh Tokens nesta versão inicial, garantindo segurança máxima com menor esforço de código.

Frente 6: Esgotamento de Conexões (Timeouts e HikariCP)
Controle o tempo máximo que o Java espera o SQL Server responder antes de abortar a operação e liberar a conexão.

Arquivo: src/main/resources/application.yml

Configurações Obrigatórias do Spring Datasource:

spring.datasource.hikari.connection-timeout: Defina para 3000 (3 segundos). Se o Java não conseguir uma conexão do pool nesse tempo, ele desiste.

spring.datasource.hikari.maximum-pool-size: Defina para 15. Evita que o Java abra centenas de conexões simultâneas e derrube o banco.

spring.jpa.properties.javax.persistence.query.timeout: Defina para 5000 (5 segundos). Se a View for lenta e o banco demorar mais de 5 segundos para calcular, o Java aborta a query no meio do caminho. É preferível que um gráfico falhe isoladamente do que a API inteira travar esperando.
