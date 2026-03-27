# Seguranca e Acesso

## Objetivo

A seguranca da plataforma combina autenticacao por JWT, refresh token rotativo, autorizacao por permissao e papel, protecao para rotas internas e rate limiting.

## Cadeia de seguranca do backend

Arquivo principal:

- `dashboard-api/src/main/java/com/dashboard/api/config/SegurancaWebConfig.java`

Filtros relevantes:

- `FiltroApiKey`
- `FiltroValidacaoJwt`
- `FiltroRateLimitApi`

Ordem efetiva:

1. API key para rotas internas
2. JWT para usuario autenticado
3. rate limiting apos autenticacao

## Rotas publicas e protegidas

Publicas:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Protegidas:

- todo `/api/painel/*`
- todo `/api/dimensoes/*`
- todo `/api/admin/acesso/*`
- `/actuator/**` fora das probes publicas

## Sessao

Configuracao atual:

- JWT com expiracao de `15 minutos`
- refresh token rotativo com expiracao padrao de `30 dias`
- refresh token mantido em cookie `HttpOnly`

Fluxo:

1. login gera JWT e refresh token;
2. UI salva o JWT em sessao local;
3. o cookie de refresh fica restrito ao path `/api/auth`;
4. ao receber `401`, o frontend tenta `/api/auth/refresh`;
5. se refresh falhar, a sessao local e descartada.

## Papeis e permissoes

Papeis relevantes no frontend:

- `admin_plataforma`
- `admin_acesso`
- `usuario_comum`

Permissoes de dashboard:

- `coletas`
- `manifestos`
- `fretes`
- `tracking`
- `faturas`
- `faturasPorCliente`
- `contasAPagar`
- `cotacoes`
- `executivo`
- `etlSaude`
- `dimensoes`

## Modulo administrativo

Backend:

- `AdminAcessoController`
- `GestaoSetorService`
- `GestaoUsuarioService`
- `PermissaoResolverService`
- `AuditService`

Frontend:

- `AdminSetoresPage.tsx`
- `AdminUsuariosPage.tsx`
- `useAdminAcesso.ts`

Principio:

- setor define baseline;
- usuario herda o baseline do setor;
- overrides individuais refinam o acesso;
- auditoria deve registrar alteracoes administrativas relevantes.

## Rotas internas por API key

`/api/interno/**` nao usa o fluxo normal de usuario final. O acesso e protegido por `X-API-KEY`.

Uso esperado:

- integracoes ETL/RPA
- tarefas operacionais server-to-server

Regra:

- nunca expor `API_KEY` para o frontend;
- nunca usar esse mecanismo para substituir ACL de usuario.

## Rate limiting

Configuracao em `application.yml`:

- tentativas de login por IP/usuario
- requests gerais de API por janela de tempo

Objetivo:

- reduzir abuso e brute force;
- manter feedback consistente ao cliente com `429`.

## Contrato de erro que a UI precisa respeitar

- `401`: sessao ausente ou expirada
- `403`: usuario autenticado sem permissao
- `408`: timeout de consulta
- `429`: limite excedido
- `503`: dependencia temporariamente indisponivel

## Regras de manutencao

- qualquer rota nova deve nascer protegida por default;
- toda permissao nova deve existir no catalogo de backend e no mapa de frontend;
- nao criar bypass de seguranca via query param ou header customizado;
- qualquer mudanca em sessao, cookie ou filtro deve vir acompanhada de documentacao e teste.
