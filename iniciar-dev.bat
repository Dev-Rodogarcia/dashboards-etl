@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ============================================================
REM Arquivo : iniciar-dev.bat
REM Papel   : Inicia backend e frontend do monorepo em paralelo.
REM AVISO IA: este e o unico modo permitido para IA subir/testar a aplicacao.
REM AVISO IA: backend 5011 e frontend 5174. Producao fica fora do fluxo da IA.
REM Uso     : iniciar-dev.bat
REM Teste   : iniciar-dev.bat --dry-run
REM ============================================================

chcp 65001 >nul
cd /d "%~dp0"

set "ROOT_DIR=%CD%"
set "BACKEND_DIR=%ROOT_DIR%\backend"
set "FRONTEND_DIR=%ROOT_DIR%\frontend"
set "BACKEND_MVNW=%BACKEND_DIR%\mvnw.cmd"
set "BACKEND_POM=%BACKEND_DIR%\pom.xml"
set "FRONTEND_PACKAGE=%FRONTEND_DIR%\package.json"
set "BACKEND_LOG_DIR=%BACKEND_DIR%\logs"
set "BACKEND_DEV_LOG=%BACKEND_LOG_DIR%\dashboard-api-dev.out.log"
set "BACKEND_DEV_ERR=%BACKEND_LOG_DIR%\dashboard-api-dev.err.log"
set "BACKEND_PID_FILE=%BACKEND_LOG_DIR%\dashboard-api-dev.pid"
set "BACKEND_PORT=5011"
set "FRONTEND_PORT=5174"
set "PROD_BACKEND_PORT=5010"
set "PROD_FRONTEND_PORT=5173"
set "FRONTEND_MODE=development"
set "FRONTEND_DEV_ENV_FILE=%ROOT_DIR%\.env.development"
set "LOCAL_API_URL=http://127.0.0.1:%BACKEND_PORT%"
set "LOCAL_FRONTEND_ORIGINS=http://127.0.0.1:%FRONTEND_PORT%,http://localhost:%FRONTEND_PORT%"
set "BACKEND_CMD=call "%BACKEND_MVNW%" -f "%BACKEND_POM%" spring-boot:run -Dspring-boot.run.profiles=dev"
set "DRY_RUN=0"

if /i "%~1"=="--dry-run" set "DRY_RUN=1"

call :prefer_java_home

echo.
echo ============================================
echo   INICIAR DASHBOARDS - DEV
echo ============================================
echo.

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

where java >nul 2>nul
if errorlevel 1 (
    echo [ERRO] Java nao encontrado no PATH.
    echo O backend precisa do Java 17 ou superior para rodar.
    pause
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERRO] npm nao encontrado no PATH.
    echo O frontend precisa do Node.js com npm instalado.
    pause
    exit /b 1
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo [ERRO] PowerShell nao encontrado no PATH.
    echo O script usa PowerShell para liberar portas presas com seguranca.
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

call :validate_dev_contract
if errorlevel 1 (
    pause
    exit /b 1
)

echo Backend dev esperado em: http://127.0.0.1:%BACKEND_PORT%
echo Frontend dev esperado em: http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] O modo dev inicia diretamente:
echo        - backend\mvnw.cmd spring-boot:run
echo        - npm run dev -- --mode %FRONTEND_MODE% --strictPort
echo [INFO] Antes de iniciar, apenas processos nas portas dev serao encerrados.
echo [INFO] O backend roda em background e grava log em backend\logs.
echo [INFO] O frontend roda neste terminal.
echo [INFO] Este modo injeta API local no frontend: %LOCAL_API_URL%
echo [INFO] Este modo libera CORS local para: %LOCAL_FRONTEND_ORIGINS%
echo [INFO] Vite carregara .env.development e recusara fallback para API publica.
echo [INFO] Este modo nao executa build, deploy ou limpeza de frontend\dist.
echo [INFO] Producao/Cloudflare permanece nas portas %PROD_BACKEND_PORT% e %PROD_FRONTEND_PORT%.
echo [INFO] Para Cloudflare/producao, use iniciar-prod.bat.
echo.

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] liberar portas %BACKEND_PORT% e %FRONTEND_PORT%
    echo [DRY-RUN] portas de producao proibidas neste script: %PROD_BACKEND_PORT% e %PROD_FRONTEND_PORT%
    echo [DRY-RUN] backend profile: dev
    echo [DRY-RUN] backend ambiente: SPRING_APPLICATION_JSON dev acima de dashboards\.env
    echo [DRY-RUN] backend CORS: %LOCAL_FRONTEND_ORIGINS%
    echo [DRY-RUN] backend comando: backend\mvnw.cmd -f backend\pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
    echo [DRY-RUN] log backend: %BACKEND_DEV_LOG%
    echo [DRY-RUN] aguardar healthcheck http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness
    echo [DRY-RUN] frontend env: %FRONTEND_DEV_ENV_FILE%
    echo [DRY-RUN] frontend neste terminal: npm run dev -- --mode %FRONTEND_MODE% --host 127.0.0.1 --port %FRONTEND_PORT% --strictPort
    echo [DRY-RUN] sem npm run build, sem deploy, sem alteracao em frontend\dist
    exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = @(%BACKEND_PORT%, %FRONTEND_PORT%);" ^
  "$forbidden = @(%PROD_BACKEND_PORT%, %PROD_FRONTEND_PORT%);" ^
  "if ($ports | Where-Object { $forbidden -contains $_ }) { Write-Host '[ERRO] Script DEV tentou operar em porta de producao.'; exit 4 }" ^
  "foreach ($port in $ports) {" ^
  "  $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "  if (-not $listeners) { Write-Host ('[OK] Porta ' + $port + ' livre.'); continue }" ^
  "  $processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "  Write-Host ('[INFO] Porta ' + $port + ' em uso. Encerrando processos...');" ^
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

if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel liberar as portas de desenvolvimento.
    pause
    exit /b 1
)

if not exist "%BACKEND_LOG_DIR%\" mkdir "%BACKEND_LOG_DIR%"
if exist "%BACKEND_DEV_LOG%" del /q "%BACKEND_DEV_LOG%" >nul 2>nul
if exist "%BACKEND_DEV_ERR%" del /q "%BACKEND_DEV_ERR%" >nul 2>nul

echo.
echo [INFO] Iniciando backend em background...
set "SPRING_PROFILES_ACTIVE=dev"
set "ENVIRONMENT=dev"
set "SERVER_ADDRESS=127.0.0.1"
set "SERVER_PORT=%BACKEND_PORT%"
set "CORS_ORIGENS_PERMITIDAS=%LOCAL_FRONTEND_ORIGINS%"
set "SECURITY_TRUST_FORWARDED_HEADERS=false"
set "AUTH_REFRESH_COOKIE_SECURE=false"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$env:SPRING_PROFILES_ACTIVE = 'dev';" ^
  "$env:ENVIRONMENT = 'dev';" ^
  "$env:SERVER_ADDRESS = '127.0.0.1';" ^
  "$env:SERVER_PORT = $env:BACKEND_PORT;" ^
  "$env:CORS_ORIGENS_PERMITIDAS = $env:LOCAL_FRONTEND_ORIGINS;" ^
  "$env:SECURITY_TRUST_FORWARDED_HEADERS = 'false';" ^
  "$env:AUTH_REFRESH_COOKIE_SECURE = 'false';" ^
  "$springConfig = [ordered]@{ spring = @{ profiles = @{ active = 'dev' } }; app = @{ environment = 'dev' }; server = @{ address = '127.0.0.1'; port = [int]$env:BACKEND_PORT }; cors = @{ 'origens-permitidas' = $env:LOCAL_FRONTEND_ORIGINS }; security = @{ 'trust-forwarded-headers' = $false }; auth = @{ 'refresh-cookie-secure' = $false } };" ^
  "$env:SPRING_APPLICATION_JSON = $springConfig | ConvertTo-Json -Compress -Depth 6;" ^
  "$process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c',$env:BACKEND_CMD) -WorkingDirectory $env:ROOT_DIR -WindowStyle Hidden -RedirectStandardOutput $env:BACKEND_DEV_LOG -RedirectStandardError $env:BACKEND_DEV_ERR -PassThru;" ^
  "Set-Content -Path $env:BACKEND_PID_FILE -Value $process.Id;" ^
  "Write-Host ('[OK] Backend iniciado em background. PID=' + $process.Id);" ^
  "Write-Host ('[INFO] Log backend: ' + $env:BACKEND_DEV_LOG);" ^
  "exit 0"

if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel iniciar o backend em background.
    pause
    exit /b 1
)

echo.
echo [INFO] Aguardando backend responder antes de iniciar o frontend...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$url = 'http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness';" ^
  "$deadline = (Get-Date).AddSeconds(120);" ^
  "while ((Get-Date) -lt $deadline) {" ^
  "  try {" ^
  "    $response = Invoke-RestMethod -Uri $url -TimeoutSec 3;" ^
  "    if ($response.status -eq 'UP') { Write-Host '[OK] Backend pronto para login.'; exit 0 }" ^
  "  } catch {}" ^
  "  Start-Sleep -Seconds 2;" ^
  "}" ^
  "Write-Host ('[ERRO] Backend nao respondeu no healthcheck: ' + $url);" ^
  "exit 1"

if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao iniciado porque o backend ainda nao ficou disponivel.
    echo Verifique os logs:
    echo   %BACKEND_DEV_LOG%
    echo   %BACKEND_DEV_ERR%
    echo.
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "Write-Host '[INFO] Ultimas linhas do log do backend:';" ^
      "Get-Content -Path $env:BACKEND_DEV_LOG -Tail 60 -ErrorAction SilentlyContinue;" ^
      "Get-Content -Path $env:BACKEND_DEV_ERR -Tail 60 -ErrorAction SilentlyContinue;"
    pause
    exit /b 1
)

echo.
echo [INFO] Iniciando frontend dev neste terminal...
echo [INFO] Use Ctrl+C para encerrar o frontend. O backend sera encerrado na proxima execucao ao liberar a porta %BACKEND_PORT%.
echo.

cd /d "%FRONTEND_DIR%"
set "DASHBOARD_FRONTEND_WINDOW=1"
set "NODE_ENV=development"
set "VITE_API_BASE_URL=%LOCAL_API_URL%"
set "API_BASE_URL=%LOCAL_API_URL%"
call npm run dev -- --mode %FRONTEND_MODE% --host 127.0.0.1 --port %FRONTEND_PORT% --strictPort
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo [INFO] Frontend dev encerrado com codigo %EXIT_CODE%.
exit /b %EXIT_CODE%

:validate_dev_contract
if not "%BACKEND_PORT%"=="5011" (
    echo [ERRO] Contrato DEV violado: BACKEND_PORT precisa ser 5011.
    exit /b 1
)

if not "%FRONTEND_PORT%"=="5174" (
    echo [ERRO] Contrato DEV violado: FRONTEND_PORT precisa ser 5174.
    exit /b 1
)

if "%BACKEND_PORT%"=="%PROD_BACKEND_PORT%" (
    echo [ERRO] Contrato DEV violado: backend tentou usar porta de producao %PROD_BACKEND_PORT%.
    exit /b 1
)

if "%FRONTEND_PORT%"=="%PROD_FRONTEND_PORT%" (
    echo [ERRO] Contrato DEV violado: frontend tentou usar porta de producao %PROD_FRONTEND_PORT%.
    exit /b 1
)

if not exist "%FRONTEND_DEV_ENV_FILE%" (
    echo [ERRO] Arquivo nao encontrado: .env.development
    echo        O Vite dev deve carregar .env.development com API local em %LOCAL_API_URL%.
    exit /b 1
)

findstr /B /L /C:"VITE_API_BASE_URL=%LOCAL_API_URL%" "%FRONTEND_DEV_ENV_FILE%" >nul
if errorlevel 1 (
    echo [ERRO] .env.development precisa definir VITE_API_BASE_URL=%LOCAL_API_URL%.
    exit /b 1
)

findstr /I /C:"api-analytics.rodogarcia.com.br" /C:"analytics.rodogarcia.com.br" "%FRONTEND_DEV_ENV_FILE%" >nul
if not errorlevel 1 (
    echo [ERRO] .env.development nao pode apontar para dominios de producao.
    exit /b 1
)

exit /b 0

:prefer_java_home
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    )
)
exit /b 0
