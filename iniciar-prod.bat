
@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ============================================================
REM Arquivo : iniciar-prod.bat
REM Papel   : Builda e inicia a producao completa em 2 terminais.
REM AVISO IA: nao execute este script. IAs devem usar somente iniciar-dev.bat.
REM AVISO IA: nao reinicie producao, nao libere 5010/5173 e nao toque dominios publicos.
REM API     : https://api-analytics.rodogarcia.com.br -> http://127.0.0.1:5010
REM UI      : https://analytics.rodogarcia.com.br     -> http://127.0.0.1:5173
REM Teste   : iniciar-prod.bat --dry-run
REM ============================================================

chcp 65001 >nul
cd /d "%~dp0"

set "ROOT_DIR=%CD%"
set "BACKEND_DIR=%ROOT_DIR%\backend"
set "FRONTEND_DIR=%ROOT_DIR%\frontend"
set "ENV_FILE=%ROOT_DIR%\.env"
set "BACKEND_MVNW=%BACKEND_DIR%\mvnw.cmd"
set "BACKEND_POM=%BACKEND_DIR%\pom.xml"
set "FRONTEND_PACKAGE=%FRONTEND_DIR%\package.json"
set "DIST_DIR=%FRONTEND_DIR%\dist"
set "VITE_CACHE_DIR=%FRONTEND_DIR%\node_modules\.vite"
set "TSCACHE_DIR=%FRONTEND_DIR%\node_modules\.tmp"
set "BUILD_INFO_FILE=%DIST_DIR%\build-info.json"
set "BACKEND_PORT=5010"
set "FRONTEND_PORT=5173"
set "BACKEND_DEV_PORT=5011"
set "FRONTEND_DEV_PORT=5174"
set "BACKEND_HEALTH_URL=http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness"
set "BACKEND_WAIT_SECONDS=180"
set "PROD_FRONTEND_PUBLIC_URL=https://analytics.rodogarcia.com.br"
set "PROD_API_PUBLIC_URL=https://api-analytics.rodogarcia.com.br"
set "DRY_RUN=0"

if /i "%~1"=="--backend-worker" goto backend_worker
if /i "%~1"=="--frontend-worker" goto frontend_worker
if /i "%~1"=="--dry-run" set "DRY_RUN=1"

call :prefer_java_home
call :print_main_header

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] Validaria Java, Node, npm, PowerShell, .env, backend e frontend.
    echo [DRY-RUN] Liberaria a UI %FRONTEND_PORT% antes do build e a API %BACKEND_PORT% antes do novo backend.
    echo [DRY-RUN] Encerraria portas de desenvolvimento antes da producao: %BACKEND_DEV_PORT% e %FRONTEND_DEV_PORT%.
    echo [DRY-RUN] Geraria build atualizado em frontend\dist antes de abrir a UI.
    echo [DRY-RUN] API publica usada no build: %PROD_API_PUBLIC_URL%
    echo [DRY-RUN] CORS esperado na API: %PROD_FRONTEND_PUBLIC_URL%
    echo [DRY-RUN] Abriria dois terminais: "Dashboard API Producao" e "Dashboard UI Producao".
    echo [DRY-RUN] Cloudflare frontend: %PROD_FRONTEND_PUBLIC_URL% -^> http://127.0.0.1:%FRONTEND_PORT%
    echo [DRY-RUN] Cloudflare API     : %PROD_API_PUBLIC_URL% -^> http://127.0.0.1:%BACKEND_PORT%
    exit /b 0
)

call :validate_common
if errorlevel 1 exit /b 1

call :load_env_file "%ENV_FILE%"
if errorlevel 1 exit /b 1

call :set_prod_env

echo [INFO] Producao local:
echo        API: http://127.0.0.1:%BACKEND_PORT%
echo        UI : http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] Cloudflare Tunnel esperado:
echo        %PROD_API_PUBLIC_URL% -^> http://127.0.0.1:%BACKEND_PORT%
echo        %PROD_FRONTEND_PUBLIC_URL% -^> http://127.0.0.1:%FRONTEND_PORT%
echo.

call :stop_dev_ports
if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel encerrar as portas de desenvolvimento.
    pause
    exit /b 1
)

call :ensure_frontend_dependencies
if errorlevel 1 exit /b 1

call :release_ports "%FRONTEND_PORT%"
if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel liberar a porta do frontend de producao.
    pause
    exit /b 1
)

call :build_frontend
if errorlevel 1 (
    echo.
    echo [ERRO] Producao nao sera iniciada porque o build do frontend falhou.
    pause
    exit /b 1
)

call :release_ports "%BACKEND_PORT%,%FRONTEND_PORT%"
if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel liberar as portas de producao.
    pause
    exit /b 1
)

echo.
echo [INFO] Abrindo backend de producao em terminal externo...
call :start_backend_window
if errorlevel 1 (
    echo [ERRO] Nao foi possivel abrir o terminal do backend.
    pause
    exit /b 1
)

echo [INFO] Aguardando backend ficar UP antes de abrir a UI...
call :wait_backend_health
if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao sera iniciado porque o backend nao confirmou healthcheck.
    echo        Confira a janela "Dashboard API Producao".
    pause
    exit /b 1
)

call :validate_backend_cors
if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao sera iniciado enquanto a API de producao estiver sem CORS correto.
    echo        Confira a janela "Dashboard API Producao" e o dashboards\.env.
    pause
    exit /b 1
)

echo.
echo [INFO] Abrindo frontend estatico de producao em terminal externo...
call :start_frontend_window
if errorlevel 1 (
    echo [ERRO] Nao foi possivel abrir o terminal do frontend.
    pause
    exit /b 1
)

echo.
echo [OK] Producao iniciada em dois terminais externos.
echo [INFO] API publica: %PROD_API_PUBLIC_URL%
echo [INFO] UI publica : %PROD_FRONTEND_PUBLIC_URL%
echo [INFO] Build frontend: %BUILD_INFO_FILE%
timeout /t 3 >nul
exit /b 0

:backend_worker
call :prefer_java_home
call :print_backend_header
call :validate_backend_worker
if errorlevel 1 (
    pause
    exit /b 1
)

call :load_env_file "%ENV_FILE%"
if errorlevel 1 (
    pause
    exit /b 1
)

call :set_prod_env

echo [INFO] Backend prod esperado em: http://127.0.0.1:%BACKEND_PORT%
echo [INFO] Profile Spring: %SPRING_PROFILES_ACTIVE%
echo [INFO] CORS liberado para: %CORS_ORIGENS_PERMITIDAS%
echo [INFO] Healthcheck: %BACKEND_HEALTH_URL%
echo.
echo [INFO] Iniciando Spring Boot em modo producao...
echo [INFO] Use Ctrl+C para encerrar esta API.
echo.

cd /d "%BACKEND_DIR%"
call .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo [INFO] Backend prod encerrado com codigo %EXIT_CODE%.
pause
exit /b %EXIT_CODE%

:frontend_worker
call :print_frontend_header
call :validate_frontend_worker
if errorlevel 1 (
    pause
    exit /b 1
)

call :set_prod_env

cd /d "%FRONTEND_DIR%"
echo [INFO] Servindo frontend estatico em http://127.0.0.1:%FRONTEND_PORT%/
echo [INFO] Publico via Cloudflare: %PROD_FRONTEND_PUBLIC_URL%
echo [INFO] API publica usada pelo build: %PROD_API_PUBLIC_URL%
echo [INFO] Use Ctrl+C para encerrar esta UI.
echo.

call npm run start:prod
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo [INFO] Frontend estatico encerrado com codigo %EXIT_CODE%.
pause
exit /b %EXIT_CODE%

:print_main_header
echo.
echo ============================================
echo   DASHBOARDS - PRODUCAO COMPLETA
echo ============================================
echo.
exit /b 0

:print_backend_header
echo.
echo ============================================
echo   DASHBOARDS - BACKEND PRODUCAO
echo ============================================
echo.
exit /b 0

:print_frontend_header
echo.
echo ============================================
echo   DASHBOARDS - FRONTEND PRODUCAO ESTATICO
echo ============================================
echo.
exit /b 0

:validate_common
if not exist "%BACKEND_DIR%\" (
    echo [ERRO] Pasta nao encontrada: backend
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%\" (
    echo [ERRO] Pasta nao encontrada: frontend
    pause
    exit /b 1
)

if not exist "%BACKEND_MVNW%" (
    echo [ERRO] Arquivo nao encontrado: backend\mvnw.cmd
    pause
    exit /b 1
)

if not exist "%BACKEND_POM%" (
    echo [ERRO] Arquivo nao encontrado: backend\pom.xml
    pause
    exit /b 1
)

if not exist "%FRONTEND_PACKAGE%" (
    echo [ERRO] Arquivo nao encontrado: frontend\package.json
    pause
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo [ERRO] Arquivo dashboards\.env nao encontrado.
    echo        Crie a partir de .env.production.example e preencha as credenciais reais.
    pause
    exit /b 1
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo [ERRO] PowerShell nao encontrado no PATH.
    pause
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERRO] npm nao encontrado no PATH.
    echo Instale o Node.js com npm antes de iniciar o frontend.
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERRO] Java nao encontrado no PATH.
    echo O backend precisa do Java 17 ou superior para rodar.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\scripts\check-java-version.ps1" -MinimumMajor 17
if errorlevel 1 (
    echo.
    echo [ERRO] Corrija o Java antes de iniciar o backend Spring.
    pause
    exit /b 1
)

exit /b 0

:validate_backend_worker
if not exist "%BACKEND_MVNW%" (
    echo [ERRO] Arquivo nao encontrado: backend\mvnw.cmd
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo [ERRO] Arquivo dashboards\.env nao encontrado.
    exit /b 1
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo [ERRO] PowerShell nao encontrado no PATH.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\scripts\check-java-version.ps1" -MinimumMajor 17
if errorlevel 1 exit /b 1

exit /b 0

:validate_frontend_worker
if not exist "%FRONTEND_PACKAGE%" (
    echo [ERRO] Arquivo nao encontrado: frontend\package.json
    exit /b 1
)

if not exist "%DIST_DIR%\index.html" (
    echo [ERRO] Build estatico nao encontrado em frontend\dist.
    echo        Execute .\iniciar-prod.bat para gerar o build e iniciar tudo.
    exit /b 1
)

call :validate_static_dist
if errorlevel 1 exit /b 1

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERRO] npm nao encontrado no PATH.
    exit /b 1
)

exit /b 0

:validate_static_dist
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$index = Join-Path $env:DIST_DIR 'index.html';" ^
  "if (-not (Test-Path -LiteralPath $index)) { Write-Host '[ERRO] dist/index.html nao encontrado.'; exit 1 }" ^
  "$indexHtml = Get-Content -LiteralPath $index -Raw;" ^
  "$devMarkers = @('/@vite/client', '/@react-refresh', '/src/main.tsx', '/src/main.jsx', '/node_modules/', '/@fs/');" ^
  "foreach ($marker in $devMarkers) { if ($indexHtml.Contains($marker)) { Write-Host ('[ERRO] dist/index.html contem marcador de Vite dev: ' + $marker); exit 2 } }" ^
  "$assetsDir = Join-Path $env:DIST_DIR 'assets';" ^
  "if (-not (Test-Path -LiteralPath $assetsDir)) { Write-Host '[ERRO] dist/assets nao encontrado.'; exit 3 }" ^
  "$assets = Get-ChildItem -LiteralPath $assetsDir -File -ErrorAction SilentlyContinue | Where-Object { @('.js', '.css') -contains $_.Extension.ToLowerInvariant() };" ^
  "if (-not $assets) { Write-Host '[ERRO] Build estatico sem assets JS/CSS em dist/assets.'; exit 4 }" ^
  "$maps = Get-ChildItem -LiteralPath $env:DIST_DIR -Recurse -Filter '*.map' -File -ErrorAction SilentlyContinue;" ^
  "if ($maps) { Write-Host '[ERRO] Build de producao contem sourcemaps .map e pode expor a arvore de fontes no DevTools.'; exit 5 }" ^
  "Write-Host '[OK] dist validado como build estatico de producao.';" ^
  "exit 0"
exit /b %ERRORLEVEL%

:release_ports
set "PORT_LIST=%~1"
if "%PORT_LIST%"=="" set "PORT_LIST=%BACKEND_PORT%,%FRONTEND_PORT%"
echo [INFO] Liberando porta(s) de producao: %PORT_LIST%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = $env:PORT_LIST -split ',' | Where-Object { $_ -match '\S' } | ForEach-Object { [int]$_.Trim() };" ^
  "$allowed = @([int]$env:BACKEND_PORT, [int]$env:FRONTEND_PORT);" ^
  "$forbidden = @([int]$env:BACKEND_DEV_PORT, [int]$env:FRONTEND_DEV_PORT);" ^
  "if ($ports | Where-Object { $forbidden -contains $_ }) { Write-Host '[ERRO] Script PROD tentou operar em porta de desenvolvimento.'; exit 4 }" ^
  "if ($ports | Where-Object { $allowed -notcontains $_ }) { Write-Host '[ERRO] Script PROD recebeu porta fora do contrato 5010/5173.'; exit 5 }" ^
  "foreach ($port in $ports) {" ^
  "  $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "  if (-not $listeners) { Write-Host ('[OK] Porta ' + $port + ' livre.'); continue }" ^
  "  $processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "  Write-Host ('[INFO] Porta ' + $port + ' em uso. Encerrando processo antigo de producao...');" ^
  "  foreach ($procId in $processIds) {" ^
  "    $process = Get-Process -Id $procId -ErrorAction SilentlyContinue;" ^
  "    if ($process) {" ^
  "      Write-Host ('[INFO] Encerrando PID=' + $procId + ' | ' + $process.ProcessName + ' | ' + $process.Path);" ^
  "      try { Stop-Process -Id $procId -Force -ErrorAction Stop } catch { Write-Host ('[ERRO] Falha ao encerrar PID=' + $procId + ': ' + $_.Exception.Message); exit 3 }" ^
  "    }" ^
  "  }" ^
  "  Start-Sleep -Milliseconds 800;" ^
  "  $restantes = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "  if ($restantes) { Write-Host ('[ERRO] Porta ' + $port + ' continuou em uso.'); exit 2 }" ^
  "  Write-Host ('[OK] Porta ' + $port + ' liberada.');" ^
  "}" ^
  "exit 0"
exit /b %ERRORLEVEL%

:stop_dev_ports
set "DEV_PORT_LIST=%BACKEND_DEV_PORT%,%FRONTEND_DEV_PORT%"
echo [INFO] Encerrando servidores DEV antes da producao: %DEV_PORT_LIST%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = $env:DEV_PORT_LIST -split ',' | Where-Object { $_ -match '\S' } | ForEach-Object { [int]$_.Trim() };" ^
  "foreach ($port in $ports) {" ^
  "  $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "  if (-not $listeners) { Write-Host ('[OK] Porta DEV ' + $port + ' livre.'); continue }" ^
  "  $processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "  foreach ($procId in $processIds) {" ^
  "    $proc = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $procId) -ErrorAction SilentlyContinue;" ^
  "    $name = if ($proc) { $proc.Name } else { 'processo' };" ^
  "    $cmd = if ($proc) { $proc.CommandLine } else { '' };" ^
  "    Write-Host ('[INFO] Encerrando porta DEV ' + $port + ' PID=' + $procId + ' | ' + $name);" ^
  "    if ($cmd) { Write-Host ('       ' + $cmd) }" ^
  "    try { Stop-Process -Id $procId -Force -ErrorAction Stop } catch { Write-Host ('[ERRO] Falha ao encerrar PID=' + $procId + ': ' + $_.Exception.Message); exit 1 }" ^
  "  }" ^
  "  Start-Sleep -Milliseconds 800;" ^
  "  $restantes = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "  if ($restantes) { Write-Host ('[ERRO] Porta DEV ' + $port + ' continuou em uso.'); exit 2 }" ^
  "  Write-Host ('[OK] Porta DEV ' + $port + ' encerrada.');" ^
  "}" ^
  "exit 0"
exit /b %ERRORLEVEL%

:ensure_frontend_dependencies
if exist "%FRONTEND_DIR%\node_modules\" exit /b 0

echo [INFO] Instalando dependencias do frontend...
cd /d "%FRONTEND_DIR%"
call npm install --legacy-peer-deps
set "INSTALL_EXIT_CODE=%ERRORLEVEL%"
cd /d "%ROOT_DIR%"

if not "%INSTALL_EXIT_CODE%"=="0" (
    echo [ERRO] npm install falhou.
    exit /b 1
)

exit /b 0

:build_frontend
echo [INFO] Limpando build anterior e caches do Vite/TypeScript...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$frontendRoot = [System.IO.Path]::GetFullPath($env:FRONTEND_DIR);" ^
  "$paths = @($env:DIST_DIR, $env:VITE_CACHE_DIR, (Join-Path $env:TSCACHE_DIR 'tsconfig.app.tsbuildinfo'), (Join-Path $env:TSCACHE_DIR 'tsconfig.node.tsbuildinfo'));" ^
  "foreach ($path in $paths) {" ^
  "  if (Test-Path -LiteralPath $path) {" ^
  "    $full = [System.IO.Path]::GetFullPath($path);" ^
  "    if (-not $full.StartsWith($frontendRoot, [System.StringComparison]::OrdinalIgnoreCase)) { Write-Host ('[ERRO] Caminho fora do frontend: ' + $full); exit 2 }" ^
  "    Write-Host ('[INFO] Removendo ' + $full);" ^
  "    Remove-Item -LiteralPath $full -Recurse -Force -ErrorAction Stop;" ^
  "  }" ^
  "}" ^
  "exit 0"

if errorlevel 1 (
    echo [ERRO] Falha ao limpar build anterior.
    exit /b 1
)

echo [INFO] Gerando build estatico de producao...
for /f %%i in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')"') do set "DASHBOARD_BUILD_ID=%%i"
set "VITE_DASHBOARD_BUILD_ID=%DASHBOARD_BUILD_ID%"
set "VITE_API_BASE_URL=%PROD_API_PUBLIC_URL%"
set "API_BASE_URL=%PROD_API_PUBLIC_URL%"

cd /d "%FRONTEND_DIR%"
call npm run build
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
cd /d "%ROOT_DIR%"

if not "%BUILD_EXIT_CODE%"=="0" (
    echo [ERRO] npm run build falhou.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "if (-not (Test-Path -LiteralPath $env:DIST_DIR)) { Write-Host '[ERRO] Pasta dist nao foi gerada.'; exit 1 }" ^
  "$index = Join-Path $env:DIST_DIR 'index.html';" ^
  "if (-not (Test-Path -LiteralPath $index)) { Write-Host '[ERRO] dist/index.html nao foi gerado.'; exit 1 }" ^
  "$indexHtml = Get-Content -LiteralPath $index -Raw;" ^
  "if (-not $indexHtml.Contains($env:DASHBOARD_BUILD_ID)) { Write-Host ('[ERRO] index.html nao referencia o build ID atual: ' + $env:DASHBOARD_BUILD_ID); exit 1 }" ^
  "$assetsDir = Join-Path $env:DIST_DIR 'assets';" ^
  "$assets = Get-ChildItem -LiteralPath $assetsDir -File -ErrorAction SilentlyContinue;" ^
  "if (-not $assets) { Write-Host '[ERRO] Build sem assets em dist/assets.'; exit 1 }" ^
  "$info = [ordered]@{ buildId = $env:DASHBOARD_BUILD_ID; builtAt = (Get-Date).ToUniversalTime().ToString('o'); frontendPort = [int]$env:FRONTEND_PORT; backendPort = [int]$env:BACKEND_PORT; frontendPublicUrl = $env:PROD_FRONTEND_PUBLIC_URL; apiPublicUrl = $env:PROD_API_PUBLIC_URL };" ^
  "$json = $info | ConvertTo-Json -Compress;" ^
  "[System.IO.File]::WriteAllText($env:BUILD_INFO_FILE, $json, (New-Object System.Text.UTF8Encoding $false));" ^
  "Write-Host ('[OK] Build novo gerado. ID=' + $env:DASHBOARD_BUILD_ID);" ^
  "Write-Host ('[OK] index.html: ' + (Get-Item -LiteralPath $index).LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'));" ^
  "exit 0"

if errorlevel 1 (
    echo [ERRO] Build de producao gerou artefatos invalidos.
    exit /b 1
)

call :validate_static_dist
if errorlevel 1 (
    echo [ERRO] Build de producao contem marcadores invalidos.
    exit /b 1
)

exit /b 0

:start_backend_window
start "Dashboard API Producao" /D "%ROOT_DIR%" "%ComSpec%" /k call "%~f0" --backend-worker
exit /b %ERRORLEVEL%

:start_frontend_window
start "Dashboard UI Producao" /D "%ROOT_DIR%" "%ComSpec%" /k call "%~f0" --frontend-worker
exit /b %ERRORLEVEL%

:wait_backend_health
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$url = $env:BACKEND_HEALTH_URL;" ^
  "$deadline = (Get-Date).AddSeconds([int]$env:BACKEND_WAIT_SECONDS);" ^
  "$tentativa = 0;" ^
  "do {" ^
  "  $tentativa++;" ^
  "  try {" ^
  "    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 4;" ^
  "    $content = $response.Content;" ^
  "    if ($content -is [byte[]]) { $content = [System.Text.Encoding]::UTF8.GetString($content) } else { $content = [string]$content }" ^
  "    $statusUp = $false;" ^
  "    try { $json = $content | ConvertFrom-Json -ErrorAction Stop; $statusUp = $json.status -eq 'UP' } catch { $statusUp = $content -match '\"status\"\s*:\s*\"UP\"' -or $content -match '\bUP\b' }" ^
  "    if ($response.StatusCode -eq 200 -and $statusUp) {" ^
  "      Write-Host ('[OK] Backend prod pronto em ' + $url);" ^
  "      exit 0;" ^
  "    }" ^
  "    Write-Host ('[INFO] Backend respondeu, mas ainda nao esta UP. Tentativa ' + $tentativa + '.');" ^
  "  } catch {" ^
  "    Write-Host ('[INFO] Aguardando backend subir... tentativa ' + $tentativa);" ^
  "  }" ^
  "  Start-Sleep -Seconds 3;" ^
  "} while ((Get-Date) -lt $deadline);" ^
  "Write-Host ('[ERRO] Backend nao ficou UP em ' + $url + ' dentro de ' + $env:BACKEND_WAIT_SECONDS + 's.');" ^
  "exit 1"
exit /b %ERRORLEVEL%

:validate_backend_cors
echo [INFO] Validando CORS da API local de producao...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$origin = $env:PROD_FRONTEND_PUBLIC_URL;" ^
  "$url = 'http://127.0.0.1:' + $env:BACKEND_PORT + '/api/auth/login';" ^
  "$headers = @{ Origin = $origin; 'Access-Control-Request-Method' = 'POST'; 'Access-Control-Request-Headers' = 'content-type' };" ^
  "$response = $null;" ^
  "try { $response = Invoke-WebRequest -Uri $url -Method OPTIONS -Headers $headers -TimeoutSec 8 -UseBasicParsing } catch { $response = $_.Exception.Response }" ^
  "if ($null -eq $response) { Write-Host ('[ERRO] Backend nao respondeu ao preflight: ' + $url); exit 1 }" ^
  "$allowOrigin = @($response.Headers['Access-Control-Allow-Origin'])[0];" ^
  "if ($allowOrigin -ne $origin) { Write-Host ('[ERRO] Backend sem CORS de producao. Esperado Access-Control-Allow-Origin=' + $origin + '; recebido=' + $allowOrigin); exit 2 }" ^
  "Write-Host ('[OK] CORS da API aceitou ' + $origin);" ^
  "exit 0"
exit /b %ERRORLEVEL%

:load_env_file
set "ENV_FILE_TO_LOAD=%~1"

if not exist "%ENV_FILE_TO_LOAD%" (
    echo [ERRO] Arquivo de ambiente nao encontrado: %ENV_FILE_TO_LOAD%
    exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE_TO_LOAD%") do (
    if not "%%~A"=="" (
        set "%%~A=%%~B"
    )
)

echo [INFO] Variaveis carregadas de dashboards\.env para esta janela.
exit /b 0

:set_prod_env
set "SPRING_PROFILES_ACTIVE=prod"
set "ENVIRONMENT=production"
set "SERVER_PORT=%BACKEND_PORT%"
set "SERVER_ADDRESS=127.0.0.1"
set "SECURITY_TRUST_FORWARDED_HEADERS=true"
set "AUTH_REFRESH_COOKIE_SECURE=true"
set "CORS_ORIGENS_PERMITIDAS=%PROD_FRONTEND_PUBLIC_URL%"
set "VITE_API_BASE_URL=%PROD_API_PUBLIC_URL%"
set "API_BASE_URL=%PROD_API_PUBLIC_URL%"
set "FRONTEND_HOST=127.0.0.1"
set "DEBUG=false"
set "LOGGING_LEVEL_ROOT=INFO"
set "LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=INFO"
set "LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_JDBC_CORE=INFO"
set "LOGGING_LEVEL_ORG_HIBERNATE_SQL=WARN"
exit /b 0

:prefer_java_home
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    )
)
exit /b 0
