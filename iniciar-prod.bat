@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ============================================================
REM Arquivo : iniciar-prod.bat
REM Papel   : Valida o ambiente e compila os artefatos de producao.
REM AVISO IA: nao execute este script, nao faca build e nao suba producao.
REM AVISO IA: runtime/restart de producao pertence somente ao operador humano via PM2.
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
set "BACKEND_JAR=%BACKEND_DIR%\target\dashboard-api-1.0.0.jar"
set "FRONTEND_PACKAGE=%FRONTEND_DIR%\package.json"
set "DIST_DIR=%FRONTEND_DIR%\dist-prod"
set "VITE_CACHE_DIR=%FRONTEND_DIR%\node_modules\.vite"
set "TSCACHE_DIR=%FRONTEND_DIR%\node_modules\.tmp"
set "BUILD_INFO_FILE=%DIST_DIR%\build-info.json"
set "BACKEND_PORT=5010"
set "FRONTEND_PORT=5173"
set "BACKEND_DEV_PORT=5011"
set "FRONTEND_DEV_PORT=5174"
set "PROD_FRONTEND_PUBLIC_URL=https://analytics.rodogarcia.com.br"
set "PROD_API_PUBLIC_URL=https://api-analytics.rodogarcia.com.br"
set "PROD_ALLOWED_BRANCH=main"
set "DRY_RUN=0"

:parse_main_args
if "%~1"=="" goto main_args_done
if /i "%~1"=="--dry-run" set "DRY_RUN=1"
shift
goto parse_main_args
:main_args_done

call :prefer_java_home
call :print_main_header

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] Validaria Java 17+, Node, npm, PowerShell, Git, .env, backend e frontend.
    echo [DRY-RUN] Exigiria branch %PROD_ALLOWED_BRANCH% e worktree limpa antes do build.
    echo [DRY-RUN] Validaria o contrato do banco via scripts\validate-prod-db-contract.ps1.
    echo [DRY-RUN] Compilaria backend com: backend\mvnw.cmd clean package -DskipTests.
    echo [DRY-RUN] Limparia dist-prod e caches do Vite/TypeScript antes do build frontend.
    echo [DRY-RUN] Geraria frontend\dist-prod e frontend\dist-prod\build-info.json.
    echo [DRY-RUN] Nao iniciaria processos, nao abriria janelas e nao tocaria nas portas 5010/5173.
    exit /b 0
)

call :validate_common
if errorlevel 1 exit /b 1

call :validate_prod_worktree
if errorlevel 1 (
    echo.
    echo [ERRO] Build de producao bloqueado por governanca do checkout.
    exit /b 1
)

call :load_env_file "%ENV_FILE%"
if errorlevel 1 exit /b 1

call :set_prod_env

call :validate_prod_database_contract
if errorlevel 1 (
    echo.
    echo [ERRO] Build de producao bloqueado: contrato do banco desalinhado.
    exit /b 1
)

echo [INFO] Artefatos de producao que serao gerados:
echo        Backend: backend\target\dashboard-api-1.0.0.jar
echo        Frontend: frontend\dist-prod
echo.
echo [INFO] Cloudflare Tunnel esperado apos restart manual do PM2:
echo        %PROD_API_PUBLIC_URL% -^> http://127.0.0.1:%BACKEND_PORT%
echo        %PROD_FRONTEND_PUBLIC_URL% -^> http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] Este script nao inicia processos e nao encerra portas de producao ou desenvolvimento.

call :build_backend
if errorlevel 1 exit /b 1

call :ensure_frontend_dependencies
if errorlevel 1 exit /b 1

call :build_frontend
if errorlevel 1 exit /b 1

echo.
echo ============================================
echo   BUILD DE PRODUCAO CONCLUIDO
echo ============================================
echo.
echo [OK] Artefatos de producao gerados com sucesso.
echo [OK] Backend JAR : %BACKEND_JAR%
echo [OK] Frontend    : %DIST_DIR%
echo [OK] Build info  : %BUILD_INFO_FILE%
echo.
echo [PROXIMO PASSO] Para aplicar as mudancas em background, execute:
echo                pm2 restart ecosystem.config.js
echo.
exit /b 0

:print_main_header
echo.
echo ============================================
echo   DASHBOARDS - BUILD DE PRODUCAO
echo ============================================
echo.
exit /b 0

:validate_common
if not exist "%BACKEND_DIR%\" (
    echo [ERRO] Pasta nao encontrada: backend
    exit /b 1
)

if not exist "%FRONTEND_DIR%\" (
    echo [ERRO] Pasta nao encontrada: frontend
    exit /b 1
)

if not exist "%BACKEND_MVNW%" (
    echo [ERRO] Arquivo nao encontrado: backend\mvnw.cmd
    exit /b 1
)

if not exist "%BACKEND_POM%" (
    echo [ERRO] Arquivo nao encontrado: backend\pom.xml
    exit /b 1
)

if not exist "%FRONTEND_PACKAGE%" (
    echo [ERRO] Arquivo nao encontrado: frontend\package.json
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo [ERRO] Arquivo dashboards\.env nao encontrado.
    echo        Crie a partir de .env.production.example e preencha as credenciais reais.
    exit /b 1
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo [ERRO] PowerShell nao encontrado no PATH.
    exit /b 1
)

where git >nul 2>nul
if errorlevel 1 (
    echo [ERRO] git nao encontrado no PATH.
    echo        Producao precisa validar o checkout antes de gerar build.
    exit /b 1
)

where node >nul 2>nul
if errorlevel 1 (
    echo [ERRO] Node.js nao encontrado no PATH.
    echo        Instale o Node.js antes de compilar o frontend.
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERRO] npm nao encontrado no PATH.
    echo        Instale o Node.js com npm antes de compilar o frontend.
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERRO] Java nao encontrado no PATH.
    echo        O backend precisa do Java 17 ou superior para build e runtime.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\scripts\check-java-version.ps1" -MinimumMajor 17
if errorlevel 1 (
    echo.
    echo [ERRO] Corrija o Java antes de compilar o backend Spring.
    exit /b 1
)

exit /b 0

:validate_prod_worktree
git -C "%ROOT_DIR%" rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
    echo [ERRO] %ROOT_DIR% nao parece ser um checkout Git.
    exit /b 1
)

set "CURRENT_BRANCH="
for /f "usebackq delims=" %%B in (`git -C "%ROOT_DIR%" branch --show-current`) do set "CURRENT_BRANCH=%%B"

if "%CURRENT_BRANCH%"=="" (
    echo [ERRO] Nao foi possivel identificar a branch atual.
    exit /b 1
)

if not "%CURRENT_BRANCH%"=="%PROD_ALLOWED_BRANCH%" (
    echo [ERRO] Build de producao exige branch %PROD_ALLOWED_BRANCH%.
    echo        Branch atual: %CURRENT_BRANCH%
    exit /b 1
)

set "WORKTREE_DIRTY="
for /f "usebackq delims=" %%S in (`git -C "%ROOT_DIR%" status --porcelain=v1 --untracked-files=normal`) do (
    set "WORKTREE_DIRTY=1"
)

if defined WORKTREE_DIRTY (
    echo [ERRO] Worktree com alteracoes locais. Commit/stash antes de gerar artefatos de producao:
    echo.
    git -C "%ROOT_DIR%" status --short
    exit /b 1
)

echo [OK] Worktree limpa na branch %CURRENT_BRANCH%.
exit /b 0

:validate_prod_database_contract
echo [INFO] Validando contrato das views de producao...
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\scripts\validate-prod-db-contract.ps1"
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

:build_backend
echo.
echo [INFO] Compilando backend Spring Boot...
cd /d "%BACKEND_DIR%"
call .\mvnw.cmd clean package -DskipTests
set "BACKEND_BUILD_EXIT_CODE=%ERRORLEVEL%"
cd /d "%ROOT_DIR%"

if not "%BACKEND_BUILD_EXIT_CODE%"=="0" (
    echo [ERRO] Build Maven do backend falhou.
    exit /b 1
)

if not exist "%BACKEND_JAR%" (
    echo [ERRO] JAR esperado nao foi gerado: %BACKEND_JAR%
    exit /b 1
)

echo [OK] Backend compilado: %BACKEND_JAR%
exit /b 0

:build_frontend
echo.
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

echo [INFO] Variaveis carregadas de dashboards\.env para este build.
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
set "FRONTEND_PORT=%FRONTEND_PORT%"
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
