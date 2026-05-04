@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ============================================================
REM Arquivo : iniciar-front.bat
REM Papel   : Inicia somente o frontend Vite em porta fixa.
REM Porta   : 5173
REM ============================================================

chcp 65001 >nul
cd /d "%~dp0"

if /i not "%DASHBOARD_FRONTEND_WINDOW%"=="1" (
    start "Dashboard UI" cmd /k "set DASHBOARD_FRONTEND_WINDOW=1&& ""%~f0"" %*"
    exit /b 0
)

set "ROOT_DIR=%CD%"
set "FRONTEND_DIR=%ROOT_DIR%\frontend"
set "ENV_FILE=%ROOT_DIR%\.env"
set "FRONTEND_PORT=5173"

echo.
echo ============================================
echo   DASHBOARDS ETL - FRONTEND
echo ============================================
echo.

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERRO] Arquivo nao encontrado: frontend\package.json
    pause
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo [AVISO] Arquivo dashboards-etl\.env nao encontrado.
    echo         O Vite usara fallback de ambiente se existir.
    echo.
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERRO] npm nao encontrado no PATH.
    echo Instale o Node.js com npm antes de iniciar o frontend.
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
  "$port = %FRONTEND_PORT%;" ^
  "$listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "if (-not $listeners) { Write-Host ('[OK] Porta ' + $port + ' livre para o frontend.'); exit 0 }" ^
  "$processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "Write-Host ('[AVISO] Porta ' + $port + ' ja esta em uso.');" ^
  "foreach ($procId in $processIds) {" ^
  "  $process = Get-Process -Id $procId -ErrorAction SilentlyContinue;" ^
  "  if ($process) { Write-Host ('PID=' + $procId + ' | ' + $process.ProcessName + ' | ' + $process.Path) }" ^
  "}" ^
  "Write-Host 'Este script nao vai trocar para 5174/5175, para nao quebrar o apontamento do Cloudflare.';" ^
  "Write-Host 'Se este ja for o Vite correto, mantenha a janela dele aberta.';" ^
  "exit 2"

if errorlevel 1 (
    echo.
    echo [ERRO] Frontend nao iniciado porque a porta %FRONTEND_PORT% nao esta livre.
    pause
    exit /b 1
)

cd /d "%FRONTEND_DIR%"

if not exist "node_modules" (
    echo [INFO] Instalando dependencias do frontend...
    call npm install
    if errorlevel 1 (
        echo [ERRO] npm install falhou.
        pause
        exit /b 1
    )
)

echo [INFO] Iniciando Vite em porta fixa: %FRONTEND_PORT%
echo [INFO] Configuracao: dashboards-etl\.env
echo [INFO] URL local: http://localhost:%FRONTEND_PORT%/
echo [INFO] Se o processo encerrar, reabra este .bat antes de mexer no Cloudflare.
echo.

call npm run dev -- --host 0.0.0.0 --port %FRONTEND_PORT% --strictPort
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo [INFO] Frontend encerrado com codigo %EXIT_CODE%.
pause
exit /b %EXIT_CODE%
