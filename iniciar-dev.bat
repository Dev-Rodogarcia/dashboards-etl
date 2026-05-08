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
set "BACKEND_SCRIPT=%ROOT_DIR%\iniciar-backend.bat"
set "FRONTEND_SCRIPT=%ROOT_DIR%\iniciar-front.bat"
set "BACKEND_PORT=5010"
set "FRONTEND_PORT=5173"
set "DRY_RUN=0"

if /i "%~1"=="--dry-run" set "DRY_RUN=1"

echo.
echo ============================================
echo   INICIAR DASHBOARDS - DEV
echo ============================================
echo.

if not exist "%BACKEND_SCRIPT%" (
    echo [ERRO] Arquivo nao encontrado: iniciar-backend.bat
    pause
    exit /b 1
)

if not exist "%FRONTEND_SCRIPT%" (
    echo [ERRO] Arquivo nao encontrado: iniciar-front.bat
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

echo Backend esperado em: http://127.0.0.1:%BACKEND_PORT%
echo Frontend esperado em: http://127.0.0.1:%FRONTEND_PORT%
echo.
echo [INFO] O modo dev usa os mesmos scripts separados:
echo        - iniciar-backend.bat
echo        - iniciar-front.bat
echo [INFO] Antes de iniciar, processos nas portas fixas serao encerrados.
echo [INFO] Este modo injeta API local no frontend. Para Cloudflare/producao,
echo        use iniciar-front.bat sem DASHBOARD_API_LOCAL para respeitar o .env.
echo.

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] liberar portas %BACKEND_PORT% e %FRONTEND_PORT%
    echo [DRY-RUN] start "Dashboard API" cmd /k "set DASHBOARD_BACKEND_WINDOW=1&& call ""%BACKEND_SCRIPT%"""
    echo [DRY-RUN] aguardar healthcheck http://127.0.0.1:%BACKEND_PORT%/actuator/health/liveness
    echo [DRY-RUN] start "Dashboard UI" cmd /k "set DASHBOARD_FRONTEND_WINDOW=1&& set DASHBOARD_API_LOCAL=1&& call ""%FRONTEND_SCRIPT%"""
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

start "Dashboard API" cmd /k "set DASHBOARD_BACKEND_WINDOW=1&& call ""%BACKEND_SCRIPT%"""

echo.
echo [INFO] Aguardando backend responder antes de abrir o frontend...
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
    echo Verifique a janela "Dashboard API" para detalhes do erro.
    pause
    exit /b 1
)

start "Dashboard UI" cmd /k "set DASHBOARD_FRONTEND_WINDOW=1&& set DASHBOARD_API_LOCAL=1&& call ""%FRONTEND_SCRIPT%"""

echo [OK] Backend pronto e frontend iniciado pelos scripts separados.
echo Feche cada janela individualmente para encerrar os processos.
echo.
exit /b 0
