# Relatorio da Reestruturacao de Usuarios, Setores, Permissoes e Sessao

Data de consolidacao: 2026-03-25

## 1. Objetivo

Esta entrega reorganizou o modulo administrativo de acesso para resolver quatro problemas centrais:

- ambiguidade entre setor, papel administrativo e override individual
- possibilidade de multiplos papeis por usuario
- fluxo de cadastro de usuario fragmentado em varias chamadas
- sessao instavel, com logout forçado quando o access token expirava

O resultado final consolida um modelo mais previsivel, com setor como baseline de acesso, usuario herdando esse baseline com excecoes apenas de negacao, papel administrativo unico por conta e sessao persistente baseada em refresh token.

## 2. Regras finais de negocio

### 2.1 Papel administrativo unico

Cada usuario passa a possuir exatamente um papel administrativo:

- `admin_plataforma`
- `admin_acesso`
- `usuario_comum`

Regras:

- `admin_plataforma` gerencia tudo e possui bypass de escopo e permissoes
- `admin_acesso` administra usuarios e setores, mas nao recebe acesso total aos dashboards
- `admin_acesso` so pode criar e editar usuarios com papel `usuario_comum`
- `admin_acesso` nao pode operar contas `admin_plataforma`
- `usuario_comum` nao possui privilegios administrativos

### 2.2 Setor como fonte de verdade do acesso base

O setor passou a ser o lugar oficial para definir:

- dashboards liberados por baseline
- escopo de filiais
- descricao e contexto organizacional

Todo usuario obrigatoriamente pertence a um setor.

### 2.3 Usuario herda o setor e so pode ser restringido

O usuario herda o acesso do setor escolhido.

Na tela de usuarios, o override individual foi simplificado para:

- `herdar`
- `negar`

Nao existe mais concessao individual por usuario neste fluxo.

### 2.4 Precedencia de permissao

Regra final por dashboard:

1. `admin_plataforma` sempre permite
2. se existir override individual `DENY`, nega
3. caso contrario, usa o baseline do setor

Regra final por dados:

- o escopo de filiais sempre vem do setor
- override individual nao amplia filial

## 3. O que foi alterado no backend

### 3.1 Contratos de autenticacao e sessao

Foram ajustados os DTOs para refletir o modelo novo:

- login passa a usar `email`
- sessao passa a expor `papel`
- sessao passa a expor `permissoesEfetivas`
- sessao passa a expor `filiaisPermitidasEfetivas`
- payload de usuario passa a exigir `email`, `papel`, `setorId`, `permissoesNegadas`
- `confirmacaoSenha` passa a existir apenas para validacao de entrada

Arquivos centrais:

- `dashboard-api/src/main/java/com/dashboard/api/dto/LoginRequestDTO.java`
- `dashboard-api/src/main/java/com/dashboard/api/dto/SessaoUsuarioDTO.java`
- `dashboard-api/src/main/java/com/dashboard/api/dto/acesso/UsuarioRequestDTO.java`
- `dashboard-api/src/main/java/com/dashboard/api/dto/acesso/UsuarioAcessoDTO.java`

### 3.2 Gestao transacional de usuarios

O cadastro e a edicao de usuario passaram a ocorrer em uma unica operacao transacional.

O service agora:

- valida senha e confirmacao
- valida politica de senha
- garante email unico
- salva setor e papel unico
- persiste apenas permissoes negadas
- revoga refresh tokens quando senha muda ou o usuario e inativado
- protege o ultimo `admin_plataforma` ativo

Arquivo central:

- `dashboard-api/src/main/java/com/dashboard/api/service/acesso/GestaoUsuarioService.java`

### 3.3 Autorizacao por papel e setor

O resolvedor de permissao foi simplificado para o modelo:

- papel unico por usuario
- setor como baseline
- override individual apenas `DENY`
- bypass total para `admin_plataforma`

Arquivo central:

- `dashboard-api/src/main/java/com/dashboard/api/service/acesso/PermissaoResolverService.java`

### 3.4 Sessao persistente com refresh token

Foi implementado o fluxo de sessao persistente com:

- access token curto
- refresh token rotativo
- armazenamento do refresh token em banco
- cookie `HttpOnly` para refresh
- revogacao no logout
- revogacao quando a conta e inativada
- revogacao quando a senha e alterada

Endpoints envolvidos:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

Arquivos centrais:

- `dashboard-api/src/main/java/com/dashboard/api/controller/AutenticacaoController.java`
- `dashboard-api/src/main/java/com/dashboard/api/service/acesso/AutenticacaoService.java`
- `dashboard-api/src/main/java/com/dashboard/api/service/acesso/RefreshTokenService.java`
- `dashboard-api/src/main/java/com/dashboard/api/model/acesso/RefreshTokenSession.java`

Complemento operacional aplicado depois da primeira rodada:

- bootstrap automatico da tabela `acesso.refresh_tokens` no startup quando o ambiente ainda nao recebeu a migracao SQL
- isso evita `503` no login apenas porque a tabela nova ainda nao existe no banco

### 3.5 Persistencia e migracao

Foi criada uma migracao para consolidar o modelo novo:

- manter apenas um papel por usuario
- remover overrides `GRANT` legados
- alinhar `login` legado com `email`
- criar a tabela de `refresh_tokens`

Arquivo central:

- `dashboard-api/src/main/resources/db/migration/V005__papel_unico_refresh_tokens_e_email_login.sql`

### 3.6 Ajustes administrativos

O modulo `/api/admin/acesso` foi alinhado ao novo fluxo:

- usuario e salvo em uma chamada unica
- exclusao funcional virou inativacao
- listagem de papeis considera governanca do operador
- atribuicao separada de papeis saiu do fluxo principal

Arquivo central:

- `dashboard-api/src/main/java/com/dashboard/api/controller/AdminAcessoController.java`

## 4. O que foi alterado no frontend

### 4.1 Tela de setores

A tela de setores foi consolidada como a origem do baseline de acesso.

Agora ela define:

- nome e descricao do setor
- filiais permitidas
- template de acesso por dashboard

Arquivo central:

- `dashboard-ui/src/pages/AdminSetoresPage.tsx`

### 4.2 Tela de usuarios

A tela de usuarios foi reestruturada para:

- usar `email` como login
- exigir senha e confirmacao na criacao
- manter senha opcional na edicao
- permitir exatamente um papel por radio button
- exibir setor como obrigatorio
- permitir apenas negacoes individuais
- mostrar resumo de baseline herdado, negacoes e acesso efetivo
- inativar usuario em vez de excluir fisicamente

Arquivo central:

- `dashboard-ui/src/pages/AdminUsuariosPage.tsx`

### 4.3 Sessao persistente no cliente

O frontend deixou de derrubar o usuario apenas pela expiracao do access token.

Agora o fluxo cliente:

- persiste a sessao em `localStorage`
- tenta `refresh` silencioso quando recebe `401`
- atualiza o token e repete a requisicao original
- so limpa a sessao se o refresh falhar, o logout for manual ou a conta estiver revogada

Arquivos centrais:

- `dashboard-ui/src/api/clienteAxios.ts`
- `dashboard-ui/src/contexts/AutenticacaoContext.tsx`
- `dashboard-ui/src/utils/gerenciadorSessao.ts`
- `dashboard-ui/src/api/endpoints/authServico.ts`

Complemento operacional aplicado depois da primeira rodada:

- o bootstrap de autenticacao nao tenta mais chamar `refresh` sem sessao local conhecida
- isso reduz o `401` inicial ruidoso no console quando o usuario ainda nao estava logado

### 4.4 Tipos e contratos

Os tipos do frontend foram alinhados ao backend para refletir:

- papel unico
- `templatePermissoes` no setor
- `permissoesEfetivas` no usuario
- `permissoesNegadas` no usuario
- `filiaisPermitidasEfetivas` na sessao

Arquivos centrais:

- `dashboard-ui/src/types/access.ts`
- `dashboard-ui/src/types/auth.ts`
- `dashboard-ui/src/utils/accessControl.ts`

## 5. Validacoes executadas

Validacoes realizadas ao final da implementacao:

- `dashboard-api`: `./mvnw.cmd test`
- `dashboard-api`: `./mvnw.cmd -q -DskipTests compile`
- `dashboard-ui`: `npm run build`

Resultado:

- backend compilando
- backend com testes passando
- frontend compilando em build de producao

## 6. Checklist manual recomendado

Depois de subir backend e frontend, validar manualmente:

1. criar setor com filiais e template de dashboards
2. criar usuario comum com email, senha, confirmacao e setor
3. confirmar que o usuario herdou o baseline do setor
4. negar um dashboard no usuario e validar que a negacao venceu
5. confirmar que a troca de setor altera o acesso efetivo
6. confirmar que `admin_acesso` nao consegue promover usuarios para `admin_plataforma`
7. confirmar que `admin_acesso` nao edita conta `admin_plataforma`
8. confirmar que logout remove a sessao
9. confirmar que refresh silencioso mantem o usuario online apos expirar o access token
10. confirmar que usuario inativado cai no proximo refresh

## 7. Observacoes importantes

- o campo `login` foi mantido no modelo para compatibilidade de persistencia, mas o fluxo funcional passou a usar `email`
- o modelo de override individual ficou intencionalmente restritivo para reduzir risco de vazamento
- o escopo por filial continua sob controle do setor, nao do usuario
- o rate limit de login passou a contar apenas falhas reais de credencial; erros internos como indisponibilidade de banco nao devem mais consumir tentativas
- documentos mais antigos que mencionam `ACL em JSON`, multiplos papeis ou `GRANT` individual devem ser considerados historicos se conflitarem com este relatorio

## 8. Resumo executivo

O modulo de acesso foi simplificado e endurecido:

- setores agora definem o acesso base
- usuarios herdam esse acesso e so podem sofrer negacoes individuais
- cada usuario possui um unico papel administrativo
- autenticacao passa a usar email
- a sessao permanece ativa por refresh token ate logout, revogacao ou inativacao

Este documento passa a ser a referencia funcional e tecnica desta reestruturacao.
