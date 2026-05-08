@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ============================================================
REM Arquivo : iniciar-backend.bat
REM Papel   : Inicia somente o backend Spring Boot em porta fixa.
REM Porta   : 5010
REM ============================================================

chcp 65001 >nul
cd /d "%~dp0"

if /i not "%DASHBOARD_BACKEND_WINDOW%"=="1" (
    start "Dashboard API" cmd /k "set DASHBOARD_BACKEND_WINDOW=1&& ""%~f0"" %*"
    exit /b 0
)

set "ROOT_DIR=%CD%"
set "BACKEND_DIR=%ROOT_DIR%\backend"
set "ENV_FILE=%ROOT_DIR%\.env"
set "BACKEND_PORT=5010"

echo.
echo ============================================
echo   DASHBOARDS ETL - BACKEND
echo ============================================
echo.

if not exist "%BACKEND_DIR%\mvnw.cmd" (
    echo [ERRO] Arquivo nao encontrado: backend\mvnw.cmd
    pause
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo [ERRO] Arquivo nao encontrado: .env
    echo Crie dashboards-etl\.env a partir de dashboards-etl\.env.example antes de iniciar a API.
    pause
    exit /b 1
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo [ERRO] PowerShell nao encontrado no PATH.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$port = %BACKEND_PORT%;" ^
  "$listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "if (-not $listeners) { Write-Host ('[OK] Porta ' + $port + ' livre para o backend.'); exit 0 }" ^
  "$processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "Write-Host ('[INFO] Porta ' + $port + ' ja esta em uso. Encerrando processos antigos do backend...');" ^
  "foreach ($procId in $processIds) {" ^
  "  $process = Get-Process -Id $procId -ErrorAction SilentlyContinue;" ^
  "  if ($process) {" ^
  "    Write-Host ('[INFO] Encerrando PID=' + $procId + ' | ' + $process.ProcessName + ' | ' + $process.Path);" ^
  "    try { Stop-Process -Id $procId -Force -ErrorAction Stop } catch { Write-Host ('[ERRO] Falha ao encerrar PID=' + $procId + ': ' + $_.Exception.Message); exit 3 }" ^
  "  }" ^
  "}" ^
  "Start-Sleep -Milliseconds 800;" ^
  "$restantes = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "if ($restantes) { Write-Host ('[ERRO] Porta ' + $port + ' continuou em uso.'); exit 2 }" ^
  "Write-Host ('[OK] Porta ' + $port + ' liberada para o backend.');" ^
  "exit 0"

if errorlevel 1 (
    echo.
    echo [ERRO] Backend nao iniciado porque a porta %BACKEND_PORT% nao pode ser liberada.
    pause
    exit /b 1
)

cd /d "%BACKEND_DIR%"

call :load_env_file "%ENV_FILE%"

echo [INFO] Iniciando Spring Boot em porta fixa: %BACKEND_PORT%
echo [INFO] Configuracao: application.yml + dashboards-etl\.env
echo [INFO] Healthcheck: http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness
echo [INFO] Use mvnw.cmd para evitar conflito com Java antigo no PATH.
echo.

call .\mvnw.cmd spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo [INFO] Backend encerrado com codigo %EXIT_CODE%.
pause
exit /b %EXIT_CODE%

:load_env_file
set "ENV_FILE=%~1"

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%~A"=="" (
        set "%%~A=%%~B"
    )
)

echo [INFO] Variaveis carregadas de dashboards-etl\.env para esta janela.
exit /b 0
