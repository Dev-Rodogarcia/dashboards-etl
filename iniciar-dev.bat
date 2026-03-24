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
set "DRY_RUN=0"

if /i "%~1"=="--dry-run" set "DRY_RUN=1"

echo.
echo ============================================
echo   INICIAR DASHBOARDS ETL - DEV
echo ============================================
echo.

if not exist "%BACKEND_DIR%\mvnw.cmd" (
    echo [ERRO] Arquivo nao encontrado: dashboard-api\mvnw.cmd
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERRO] Arquivo nao encontrado: dashboard-ui\package.json
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

if not exist "%BACKEND_DIR%\.env" (
    echo [AVISO] Arquivo dashboard-api\.env nao encontrado.
    echo         O backend ainda pode subir se as variaveis estiverem no ambiente.
    echo.
)

echo Backend esperado em: http://localhost:8080
echo Frontend esperado em: http://localhost:5173
echo.

set "BACKEND_CMD=cd /d ""%BACKEND_DIR%"" && call .\mvnw.cmd spring-boot:run"
set "FRONTEND_CMD=cd /d ""%FRONTEND_DIR%"" && if exist node_modules (call npm run dev) else (echo [INFO] Instalando dependencias do frontend... && call npm install && call npm run dev)"

if "%DRY_RUN%"=="1" (
    echo [DRY-RUN] start "Dashboard API" cmd /k "%BACKEND_CMD%"
    echo [DRY-RUN] start "Dashboard UI" cmd /k "%FRONTEND_CMD%"
    exit /b 0
)

start "Dashboard API" cmd /k "%BACKEND_CMD%"
start "Dashboard UI" cmd /k "%FRONTEND_CMD%"

echo [OK] Frontend e backend foram iniciados em janelas separadas.
echo Feche cada janela individualmente para encerrar os processos.
echo.
exit /b 0
