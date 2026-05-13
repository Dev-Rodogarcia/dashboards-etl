@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ============================================================
REM Arquivo : iniciar-dev.bat
REM Papel   : Inicia backend e frontend do monorepo em paralelo.
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

echo Backend dev esperado em: http://127.0.0.1:%BACKEND_PORT%
echo Frontend dev esperado em: http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] O modo dev inicia diretamente:
echo        - backend\mvnw.cmd spring-boot:run
echo        - npm run dev
echo [INFO] Antes de iniciar, apenas processos nas portas dev serao encerrados.
echo [INFO] O backend roda em background e grava log em backend\logs.
echo [INFO] O frontend roda neste terminal.
echo [INFO] Este modo injeta API local no frontend: %LOCAL_API_URL%
echo [INFO] Este modo libera CORS local para: %LOCAL_FRONTEND_ORIGINS%
echo [INFO] Producao/Cloudflare permanece nas portas 5010 e 5173.
echo [INFO] Para Cloudflare/producao, use iniciar-prod.bat.
echo.

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] liberar portas %BACKEND_PORT% e %FRONTEND_PORT%
    echo [DRY-RUN] backend profile: dev
    echo [DRY-RUN] backend CORS: %LOCAL_FRONTEND_ORIGINS%
    echo [DRY-RUN] backend comando: backend\mvnw.cmd -f backend\pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
    echo [DRY-RUN] log backend: %BACKEND_DEV_LOG%
    echo [DRY-RUN] aguardar healthcheck http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness
    echo [DRY-RUN] frontend neste terminal: npm run dev -- --host 127.0.0.1 --port %FRONTEND_PORT%
    exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = @(%BACKEND_PORT%, %FRONTEND_PORT%);" ^
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
set "SERVER_PORT=%BACKEND_PORT%"
set "CORS_ORIGENS_PERMITIDAS=%LOCAL_FRONTEND_ORIGINS%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$env:SPRING_PROFILES_ACTIVE = 'dev';" ^
  "$env:ENVIRONMENT = 'dev';" ^
  "$env:SERVER_PORT = $env:BACKEND_PORT;" ^
  "$env:CORS_ORIGENS_PERMITIDAS = $env:LOCAL_FRONTEND_ORIGINS;" ^
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
set "VITE_API_BASE_URL=%LOCAL_API_URL%"
call npm run dev -- --host 127.0.0.1 --port %FRONTEND_PORT%
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo [INFO] Frontend dev encerrado com codigo %EXIT_CODE%.
exit /b %EXIT_CODE%

:prefer_java_home
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    )
)
exit /b 0
