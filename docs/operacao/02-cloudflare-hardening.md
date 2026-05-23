# Hardening Cloudflare para Dashboards em Produção

Ambiente oficial:

- Frontend: `https://analytics.rodogarcia.com.br`
- API: `https://api-analytics.rodogarcia.com.br`
- Tunnel frontend: `http://127.0.0.1:5173`
- Tunnel API: `http://127.0.0.1:5010`

## Regras WAF imediatas

No host `analytics.rodogarcia.com.br`, bloquear:

- `/@vite/*`
- `/@react-refresh`
- `/src/*`
- `/node_modules/*`
- `/@fs/*`
- `/vite.svg`

Esses caminhos pertencem ao Vite Dev Server e não devem existir quando o frontend estiver servido a partir de `frontend/dist-prod`.

No host `api-analytics.rodogarcia.com.br`, bloquear requests com header `Origin` local:

- `http://localhost:*`
- `http://127.0.0.1:*`
- `http://0.0.0.0:*`
- `http://[::1]:*`

## Cache

- Cachear apenas assets estáticos versionados do frontend, especialmente `/assets/*`.
- Não cachear `/api/*`.
- Não cachear respostas de autenticação, refresh, logout ou endpoints administrativos.

## Rate limiting

Regras sugeridas na borda, mantendo o rate limit do backend ativo:

- `/api/auth/login`: limite mais rígido por IP.
- `/api/*`: limite moderado por IP para reduzir abuso e ruído.
- `/api/*/exportacao*`: limite moderado/baixo para proteger queries e downloads.

## Headers

Aplicar no frontend:

```text
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
Content-Security-Policy: default-src 'self'; script-src 'self'; connect-src 'self' https://api-analytics.rodogarcia.com.br; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' data: https://fonts.gstatic.com; object-src 'none'; base-uri 'self'; frame-ancestors 'none'
```

Aplicar na API:

```text
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
Referrer-Policy: no-referrer
Cache-Control: no-store
```

## Validação pós-deploy

Executar após trocar o frontend para estático:

```powershell
curl.exe -I https://analytics.rodogarcia.com.br/@vite/client
curl.exe -I https://analytics.rodogarcia.com.br/src/main.tsx
curl.exe -I https://analytics.rodogarcia.com.br/
curl.exe -i -X OPTIONS https://api-analytics.rodogarcia.com.br/api/auth/login -H "Origin: https://analytics.rodogarcia.com.br" -H "Access-Control-Request-Method: POST"
curl.exe -i -X OPTIONS https://api-analytics.rodogarcia.com.br/api/auth/login -H "Origin: http://localhost:5174" -H "Access-Control-Request-Method: POST"
```

Critério esperado:

- caminhos Vite retornam `404` ou `403`;
- HTML inicial não referencia `/@vite/client`, `/@react-refresh` ou `/src/main.tsx`;
- origem de produção é aceita no CORS;
- origem local é rejeitada no CORS de produção.
