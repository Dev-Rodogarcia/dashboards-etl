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
set "BACKEND_DIR=%ROOT_DIR%\dashboard-api"
set "FRONTEND_DIR=%ROOT_DIR%\dashboard-ui"
set "BACKEND_SCRIPT=%ROOT_DIR%\iniciar-backend.bat"
set "FRONTEND_SCRIPT=%ROOT_DIR%\iniciar-front.bat"
set "BACKEND_PORT=5010"
set "FRONTEND_PORT=5173"
set "DRY_RUN=0"

if /i "%~1"=="--dry-run" set "DRY_RUN=1"

echo.
echo ============================================
echo   INICIAR DASHBOARDS ETL - DEV
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

echo Backend esperado em: http://localhost:%BACKEND_PORT%
echo Frontend esperado em: http://localhost:%FRONTEND_PORT%
echo.
echo [INFO] O modo dev usa os mesmos scripts separados:
echo        - iniciar-backend.bat
echo        - iniciar-front.bat
echo.

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] start "Dashboard API" cmd /k "set DASHBOARD_BACKEND_WINDOW=1&& call ""%BACKEND_SCRIPT%"""
    echo [DRY-RUN] start "Dashboard UI" cmd /k "set DASHBOARD_FRONTEND_WINDOW=1&& call ""%FRONTEND_SCRIPT%"""
    exit /b 0
)

start "Dashboard API" cmd /k "set DASHBOARD_BACKEND_WINDOW=1&& call ""%BACKEND_SCRIPT%"""
start "Dashboard UI" cmd /k "set DASHBOARD_FRONTEND_WINDOW=1&& call ""%FRONTEND_SCRIPT%"""

echo [OK] Frontend e backend foram iniciados pelos scripts separados.
echo Feche cada janela individualmente para encerrar os processos.
echo.
exit /b 0
