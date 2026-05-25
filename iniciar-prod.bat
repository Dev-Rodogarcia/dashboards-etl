
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
set "BACKEND_LOG_DIR=%BACKEND_DIR%\logs"
set "DIST_DIR=%FRONTEND_DIR%\dist-prod"
set "VITE_CACHE_DIR=%FRONTEND_DIR%\node_modules\.vite"
set "TSCACHE_DIR=%FRONTEND_DIR%\node_modules\.tmp"
set "BUILD_INFO_FILE=%DIST_DIR%\build-info.json"
set "PROD_START_LOCK_FILE=%BACKEND_LOG_DIR%\dashboard-prod-start.lock"
set "BACKEND_PORT=5010"
set "FRONTEND_PORT=5173"
set "BACKEND_DEV_PORT=5011"
set "FRONTEND_DEV_PORT=5174"
set "BACKEND_HEALTH_URL=http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness"
set "BACKEND_WAIT_SECONDS=180"
set "PROD_FRONTEND_PUBLIC_URL=https://analytics.rodogarcia.com.br"
set "PROD_API_PUBLIC_URL=https://api-analytics.rodogarcia.com.br"
set "PROD_ALLOWED_BRANCH=main"
set "DRY_RUN=0"

if /i "%~1"=="--backend-worker" goto backend_worker
if /i "%~1"=="--frontend-worker" goto frontend_worker

:parse_main_args
if "%~1"=="" goto main_args_done
if /i "%~1"=="--dry-run" set "DRY_RUN=1"
shift
goto parse_main_args
:main_args_done

call :prefer_java_home
call :print_main_header

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] Validaria Java, Node, npm, PowerShell, .env, backend e frontend.
    echo [DRY-RUN] Avisaria se a branch nao fosse %PROD_ALLOWED_BRANCH% ou se houvesse alteracoes locais, mas seguiria com o build.
    echo [DRY-RUN] Liberaria a UI %FRONTEND_PORT% antes do build e a API %BACKEND_PORT% antes do novo backend.
    echo [DRY-RUN] Nao encerraria nem tocaria portas de desenvolvimento: %BACKEND_DEV_PORT% e %FRONTEND_DEV_PORT%.
    echo [DRY-RUN] Geraria build atualizado em frontend\dist-prod antes de abrir a UI.
    echo [DRY-RUN] Carimbaria build-info.json com horario do build e do deploy.
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

call :validate_prod_database_contract
if errorlevel 1 (
    echo.
    echo [ERRO] Producao nao sera iniciada enquanto o contrato do banco estiver desalinhado.
    pause
    exit /b 1
)

call :validate_prod_worktree
if errorlevel 1 (
    echo.
    echo [ERRO] Producao nao sera iniciada a partir deste checkout.
    pause
    exit /b 1
)

echo [INFO] Producao local:
echo        API: http://127.0.0.1:%BACKEND_PORT%
echo        UI : http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] Cloudflare Tunnel esperado:
echo        %PROD_API_PUBLIC_URL% -^> http://127.0.0.1:%BACKEND_PORT%
echo        %PROD_FRONTEND_PUBLIC_URL% -^> http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] As portas DEV %BACKEND_DEV_PORT%/%FRONTEND_DEV_PORT% nao serao encerradas por este script.

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

call :acquire_prod_start_lock
if errorlevel 1 (
    echo.
    echo [ERRO] Producao nao sera iniciada porque outro start ainda esta em andamento.
    pause
    exit /b 1
)

echo.
echo [INFO] Abrindo backend de producao em terminal externo...
call :start_backend_window
if errorlevel 1 (
    echo [ERRO] Nao foi possivel abrir o terminal do backend.
    call :release_prod_start_lock
    pause
    exit /b 1
)

echo [INFO] Aguardando backend ficar UP antes de abrir a UI...
call :wait_backend_health
if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao sera iniciado porque o backend nao confirmou healthcheck.
    echo        Confira a janela "Dashboard API Producao".
    call :release_prod_start_lock
    pause
    exit /b 1
)

call :validate_backend_cors
if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao sera iniciado enquanto a API de producao estiver sem CORS correto.
    echo        Confira a janela "Dashboard API Producao" e o dashboards\.env.
    call :release_prod_start_lock
    pause
    exit /b 1
)

call :mark_deploy_started
if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao sera iniciado porque nao foi possivel registrar o horario do deploy.
    call :release_prod_start_lock
    pause
    exit /b 1
)

echo.
echo [INFO] Abrindo frontend estatico de producao em terminal externo...
call :start_frontend_window
if errorlevel 1 (
    echo [ERRO] Nao foi possivel abrir o terminal do frontend.
    call :release_prod_start_lock
    pause
    exit /b 1
)

call :release_prod_start_lock

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

call :skip_backend_worker_if_port_healthy
if errorlevel 2 (
    pause
    exit /b 1
)
if errorlevel 1 (
    echo.
    echo [INFO] Backend prod ja estava ativo; esta janela nao iniciara outro Spring Boot.
    pause
    exit /b 0
)

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
    echo [ERRO] Build estatico nao encontrado em frontend\dist-prod.
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
  "if (-not (Test-Path -LiteralPath $index)) { Write-Host ('[ERRO] index.html nao encontrado em ' + $env:DIST_DIR); exit 1 }" ^
  "$indexHtml = Get-Content -LiteralPath $index -Raw;" ^
  "$devMarkers = @('/@vite/client', '/@react-refresh', '/src/main.tsx', '/src/main.jsx', '/node_modules/', '/@fs/');" ^
  "foreach ($marker in $devMarkers) { if ($indexHtml.Contains($marker)) { Write-Host ('[ERRO] index.html contem marcador de Vite dev: ' + $marker); exit 2 } }" ^
  "$assetsDir = Join-Path $env:DIST_DIR 'assets';" ^
  "if (-not (Test-Path -LiteralPath $assetsDir)) { Write-Host ('[ERRO] assets nao encontrado em ' + $assetsDir); exit 3 }" ^
  "$assets = Get-ChildItem -LiteralPath $assetsDir -File -ErrorAction SilentlyContinue | Where-Object { @('.js', '.css') -contains $_.Extension.ToLowerInvariant() };" ^
  "if (-not $assets) { Write-Host ('[ERRO] Build estatico sem assets JS/CSS em ' + $assetsDir); exit 4 }" ^
  "$maps = Get-ChildItem -LiteralPath $env:DIST_DIR -Recurse -Filter '*.map' -File -ErrorAction SilentlyContinue;" ^
  "if ($maps) { Write-Host '[ERRO] Build de producao contem sourcemaps .map e pode expor a arvore de fontes no DevTools.'; exit 5 }" ^
  "$buildInfo = Join-Path $env:DIST_DIR 'build-info.json';" ^
  "if (-not (Test-Path -LiteralPath $buildInfo)) { Write-Host ('[ERRO] build-info.json nao encontrado em ' + $env:DIST_DIR + '. Gere producao somente via iniciar-prod.bat.'); exit 6 }" ^
  "Write-Host ('[OK] ' + $env:DIST_DIR + ' validado como build estatico de producao.');" ^
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

:validate_prod_worktree
where git >nul 2>nul
if errorlevel 1 (
    echo [ERRO] git nao encontrado no PATH.
    echo        Producao precisa validar o checkout antes de gerar build.
    exit /b 1
)

git -C "%ROOT_DIR%" rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
    echo [ERRO] %ROOT_DIR% nao parece ser um checkout Git.
    exit /b 1
)

set "CURRENT_BRANCH="
for /f "usebackq delims=" %%B in (`git -C "%ROOT_DIR%" branch --show-current`) do set "CURRENT_BRANCH=%%B"

if "%CURRENT_BRANCH%"=="" (
    echo [AVISO] Nao foi possivel identificar a branch atual.
    echo         Seguindo com o checkout atual mesmo assim.
    set "CURRENT_BRANCH=desconhecida"
)

if not "%CURRENT_BRANCH%"=="%PROD_ALLOWED_BRANCH%" (
    echo [AVISO] Producao iniciada fora da branch %PROD_ALLOWED_BRANCH%.
    echo         Branch atual: %CURRENT_BRANCH%
    echo         Seguindo com o build de producao a partir deste checkout.
)

set "WORKTREE_DIRTY="
for /f "usebackq delims=" %%S in (`git -C "%ROOT_DIR%" status --porcelain=v1 --untracked-files=normal`) do (
    set "WORKTREE_DIRTY=1"
)

if defined WORKTREE_DIRTY (
    echo [AVISO] Worktree com alteracoes locais.
    echo         Build de producao usara exatamente o checkout atual:
    echo.
    git -C "%ROOT_DIR%" status --short
    echo.
    echo [AVISO] Seguindo com build de producao sem exigir commit/stash.
    exit /b 0
)

echo [OK] Worktree limpa na branch %CURRENT_BRANCH%.
exit /b 0

:validate_prod_database_contract
echo [INFO] Validando contrato das views de producao...
where SQLCMD.EXE >nul 2>nul
if errorlevel 1 (
    echo [ERRO] SQLCMD.EXE nao encontrado no PATH.
    echo        Instale as ferramentas de linha de comando do SQL Server para validar producao.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\scripts\validate-prod-db-contract.ps1"
exit /b %ERRORLEVEL%

:acquire_prod_start_lock
if not exist "%BACKEND_LOG_DIR%\" mkdir "%BACKEND_LOG_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$lock = $env:PROD_START_LOCK_FILE;" ^
  "$dir = Split-Path -Parent $lock;" ^
  "if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }" ^
  "if (Test-Path -LiteralPath $lock) {" ^
  "  $age = (Get-Date) - (Get-Item -LiteralPath $lock).LastWriteTime;" ^
  "  if ($age.TotalMinutes -lt 10) { Write-Host ('[ERRO] Ja existe um start de producao em andamento: ' + $lock); exit 2 }" ^
  "  Write-Host ('[AVISO] Removendo lock antigo de start de producao: ' + $lock);" ^
  "  Remove-Item -LiteralPath $lock -Force -ErrorAction Stop;" ^
  "}" ^
  "$stream = $null;" ^
  "try {" ^
  "  $stream = [System.IO.File]::Open($lock, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None);" ^
  "  $writer = New-Object System.IO.StreamWriter($stream, [System.Text.Encoding]::UTF8);" ^
  "  $stream = $null;" ^
  "  $writer.WriteLine(('pid=' + $PID));" ^
  "  $writer.WriteLine(('createdAt=' + (Get-Date).ToString('o')));" ^
  "  $writer.Close();" ^
  "  Write-Host ('[OK] Lock de start criado: ' + $lock);" ^
  "  exit 0;" ^
  "} catch [System.IO.IOException] {" ^
  "  Write-Host ('[ERRO] Outro start criou o lock ao mesmo tempo: ' + $lock);" ^
  "  exit 3;" ^
  "} finally {" ^
  "  if ($stream) { $stream.Dispose() }" ^
  "}"
exit /b %ERRORLEVEL%

:release_prod_start_lock
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$lock = $env:PROD_START_LOCK_FILE;" ^
  "if (Test-Path -LiteralPath $lock) { Remove-Item -LiteralPath $lock -Force -ErrorAction SilentlyContinue; Write-Host ('[OK] Lock de start removido: ' + $lock) }" ^
  "exit 0"
exit /b 0

:skip_backend_worker_if_port_healthy
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$port = [int]$env:BACKEND_PORT;" ^
  "$url = $env:BACKEND_HEALTH_URL;" ^
  "$listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "if (-not $listeners) { exit 0 }" ^
  "$pids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "try {" ^
  "  $response = Invoke-RestMethod -Uri $url -TimeoutSec 5;" ^
  "  if ($response.status -eq 'UP') { Write-Host ('[AVISO] Porta ' + $port + ' ja tem backend saudavel. PID(s)=' + ($pids -join ', ')); exit 1 }" ^
  "} catch {}" ^
  "Write-Host ('[ERRO] Porta ' + $port + ' esta ocupada, mas o healthcheck nao esta UP. PID(s)=' + ($pids -join ', '));" ^
  "exit 2"
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

echo [INFO] Gerando build estatico de producao em frontend\dist-prod...
for /f %%i in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')"') do set "DASHBOARD_BUILD_ID=%%i"
set "VITE_DASHBOARD_BUILD_ID=%DASHBOARD_BUILD_ID%"
set "VITE_API_BASE_URL=%PROD_API_PUBLIC_URL%"
set "API_BASE_URL=%PROD_API_PUBLIC_URL%"

cd /d "%FRONTEND_DIR%"
call npm run build:prod
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
cd /d "%ROOT_DIR%"

if not "%BUILD_EXIT_CODE%"=="0" (
    echo [ERRO] npm run build:prod falhou.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "if (-not (Test-Path -LiteralPath $env:DIST_DIR)) { Write-Host ('[ERRO] Pasta de producao nao foi gerada: ' + $env:DIST_DIR); exit 1 }" ^
  "$index = Join-Path $env:DIST_DIR 'index.html';" ^
  "if (-not (Test-Path -LiteralPath $index)) { Write-Host ('[ERRO] index.html nao foi gerado em ' + $env:DIST_DIR); exit 1 }" ^
  "$indexHtml = Get-Content -LiteralPath $index -Raw;" ^
  "if (-not $indexHtml.Contains($env:DASHBOARD_BUILD_ID)) { Write-Host ('[ERRO] index.html nao referencia o build ID atual: ' + $env:DASHBOARD_BUILD_ID); exit 1 }" ^
  "$assetsDir = Join-Path $env:DIST_DIR 'assets';" ^
  "$assets = Get-ChildItem -LiteralPath $assetsDir -File -ErrorAction SilentlyContinue;" ^
  "if (-not $assets) { Write-Host ('[ERRO] Build sem assets em ' + $assetsDir); exit 1 }" ^
  "$info = [ordered]@{ buildId = $env:DASHBOARD_BUILD_ID; builtAt = (Get-Date).ToUniversalTime().ToString('o'); distDir = $env:DIST_DIR; frontendPort = [int]$env:FRONTEND_PORT; backendPort = [int]$env:BACKEND_PORT; frontendPublicUrl = $env:PROD_FRONTEND_PUBLIC_URL; apiPublicUrl = $env:PROD_API_PUBLIC_URL };" ^
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

:mark_deploy_started
echo [INFO] Registrando horario do deploy em %BUILD_INFO_FILE%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$path = $env:BUILD_INFO_FILE;" ^
  "if (-not (Test-Path -LiteralPath $path)) { Write-Host ('[ERRO] build-info.json nao encontrado: ' + $path); exit 1 }" ^
  "try { $info = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json -ErrorAction Stop } catch { Write-Host ('[ERRO] build-info.json invalido: ' + $_.Exception.Message); exit 2 }" ^
  "$agoraUtc = (Get-Date).ToUniversalTime().ToString('o');" ^
  "$agoraLocal = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz');" ^
  "$info | Add-Member -NotePropertyName deployedAt -NotePropertyValue $agoraUtc -Force;" ^
  "$info | Add-Member -NotePropertyName deployedAtLocal -NotePropertyValue $agoraLocal -Force;" ^
  "$json = $info | ConvertTo-Json -Compress -Depth 6;" ^
  "[System.IO.File]::WriteAllText($path, $json, (New-Object System.Text.UTF8Encoding $false));" ^
  "Write-Host ('[OK] Deploy registrado em build-info.json: ' + $agoraLocal);" ^
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
set "FRONTEND_DIST_DIR=%DIST_DIR%"
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
