@echo off
SETLOCAL EnableExtensions DisableDelayedExpansion

REM ================================================================
REM Script : database/executar_database.bat
REM Papel  : Atualiza o banco proprio do Dashboard via Flyway.
REM
REM Modos:
REM   executar_database.bat
REM      Aplica migrations pendentes em database\migrations e roda validacoes.
REM
REM   executar_database.bat --info
REM      Mostra o estado Flyway sem aplicar migrations.
REM
REM   executar_database.bat --validate-only
REM      Roda apenas as validacoes SQL operacionais.
REM
REM   executar_database.bat --no-validation
REM      Aplica Flyway sem rodar validacoes via sqlcmd.
REM
REM   executar_database.bat --env-file .env
REM      Carrega um arquivo .env especifico antes de executar.
REM
REM O alvo deve ser DASHBOARDS ou DASHBOARDS_DEV, salvo quando
REM DASHBOARDS_DB_ALLOW_CUSTOM=1 estiver configurado para sandbox controlado.
REM Nunca use este script contra ETL_SISTEMA.
REM ================================================================

chcp 65001 >nul

set "DB_DIR=%~dp0"
for %%I in ("%DB_DIR%..") do set "DASHBOARD_ROOT=%%~fI"
cd /d "%DASHBOARD_ROOT%"

set "MODE_INFO=0"
set "MODE_VALIDATE_ONLY=0"
set "RUN_VALIDATIONS=1"
set "ENV_FILE_ARG="

:PARSE_ARGS
if "%~1"=="" goto :ARGS_OK
if /i "%~1"=="--help" goto :MOSTRAR_AJUDA
if /i "%~1"=="-h" goto :MOSTRAR_AJUDA
if /i "%~1"=="/?" goto :MOSTRAR_AJUDA
if /i "%~1"=="--info" (
    set "MODE_INFO=1"
    shift
    goto :PARSE_ARGS
)
if /i "%~1"=="--validate-only" (
    set "MODE_VALIDATE_ONLY=1"
    shift
    goto :PARSE_ARGS
)
if /i "%~1"=="--no-validation" (
    set "RUN_VALIDATIONS=0"
    shift
    goto :PARSE_ARGS
)
if /i "%~1"=="--env-file" goto :ARG_ENV_FILE

echo [ERRO] Argumento desconhecido: %~1
echo Use --help para ver as opcoes.
exit /b 1

:ARG_ENV_FILE
shift
if "%~1"=="" (
    echo [ERRO] Informe o caminho apos --env-file.
    exit /b 1
)
set "ENV_FILE_ARG=%~1"
shift
goto :PARSE_ARGS

:ARGS_OK

echo.
echo ============================================
echo   DASHBOARDS DATABASE - FLYWAY
echo ============================================
echo.

if not exist "database\migrations\" (
    echo [ERRO] Pasta database\migrations nao encontrada em %DASHBOARD_ROOT%.
    exit /b 1
)

if defined ENV_FILE_ARG (
    call :LOAD_ENV "%ENV_FILE_ARG%"
    if errorlevel 1 exit /b 1
) else if exist "database\config.bat" (
    echo Carregando configuracao database\config.bat
    call "database\config.bat"
) else if defined DASHBOARDS_ENV_FILE (
    call :LOAD_ENV "%DASHBOARDS_ENV_FILE%"
    if errorlevel 1 exit /b 1
) else if exist ".env.development.local" (
    call :LOAD_ENV ".env.development.local"
    if errorlevel 1 exit /b 1
) else if exist ".env" (
    call :LOAD_ENV ".env"
    if errorlevel 1 exit /b 1
) else (
    echo [ERRO] Nenhuma configuracao encontrada.
    echo Copie database\config_exemplo.bat para database\config.bat ou use --env-file.
    exit /b 1
)

if "%DB_URL%"=="" (
    if "%DB_SERVER%"=="" (
        echo [ERRO] DB_URL ou DB_SERVER precisa estar configurado.
        exit /b 1
    )
    if "%DB_NAME%"=="" (
        echo [ERRO] DB_URL ou DB_NAME precisa estar configurado.
        exit /b 1
    )
    set "DB_JDBC_SERVER=%DB_SERVER%"
    if not "%DB_PORT%"=="" set "DB_JDBC_SERVER=%DB_SERVER%:%DB_PORT%"
    set "DB_URL=jdbc:sqlserver://%DB_JDBC_SERVER%;databaseName=%DB_NAME%;encrypt=true;trustServerCertificate=true"
)

call :RESOLVE_DB_NAME_FROM_URL
if "%DB_NAME%"=="" if not "%DB_NAME_FROM_URL%"=="" set "DB_NAME=%DB_NAME_FROM_URL%"
if not "%DB_NAME_FROM_URL%"=="" if /i not "%DB_NAME%"=="%DB_NAME_FROM_URL%" (
    echo [ERRO] DB_NAME diverge do databaseName informado em DB_URL.
    echo DB_NAME=%DB_NAME%
    echo DB_URL databaseName=%DB_NAME_FROM_URL%
    exit /b 1
)
if "%DB_SERVER_TARGET%"=="" (
    if not "%DB_SERVER%"=="" (
        set "DB_SERVER_TARGET=%DB_SERVER%"
        if not "%DB_PORT%"=="" set "DB_SERVER_TARGET=%DB_SERVER%,%DB_PORT%"
    ) else (
        call :RESOLVE_SQLCMD_SERVER
    )
)

if "%DB_NAME%"=="" (
    echo [ERRO] Nao foi possivel identificar databaseName/database no DB_URL.
    exit /b 1
)

call :VALIDATE_TARGET
if errorlevel 1 exit /b 1

if not exist "backend\mvnw.cmd" (
    echo [ERRO] backend\mvnw.cmd nao encontrado.
    exit /b 1
)

echo Ambiente: %ENVIRONMENT% / %SPRING_PROFILES_ACTIVE%
echo Database: %DB_NAME%
echo JDBC URL: %DB_URL%
if "%DB_USER%"=="" (
    echo Auth Flyway: definida pela JDBC URL
) else (
    echo Auth Flyway: SQL Login ^(%DB_USER%^)
)
if not "%DB_SERVER_TARGET%"=="" echo SQLCMD target: %DB_SERVER_TARGET%
echo.

if "%MODE_VALIDATE_ONLY%"=="1" goto :RUN_VALIDATIONS

if "%MODE_INFO%"=="1" (
    call :RUN_FLYWAY info
    exit /b %ERRORLEVEL%
)

call :RUN_FLYWAY migrate
if errorlevel 1 exit /b 1

if "%RUN_VALIDATIONS%"=="1" (
    call :RUN_VALIDATIONS
    if errorlevel 1 exit /b 1
) else (
    echo [INFO] Validacoes SQL ignoradas por --no-validation.
)

echo.
echo ============================================
echo   CONCLUIDO - Dashboard database atualizado.
echo ============================================
echo.
exit /b 0

:LOAD_ENV
set "ENV_FILE=%~1"
if not exist "%ENV_FILE%" (
    echo [ERRO] Arquivo de ambiente nao encontrado: %ENV_FILE%
    exit /b 1
)
echo Carregando configuracao %ENV_FILE%
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%A"=="" set "%%A=%%B"
)
exit /b 0

:RESOLVE_DB_NAME_FROM_URL
set "DB_NAME_FROM_URL="
for /f "usebackq delims=" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$u=$env:DB_URL; if ($u -match '(?i)(?:^|;)databaseName=([^;]+)') { $Matches[1] } elseif ($u -match '(?i)(?:^|;)database=([^;]+)') { $Matches[1] }"`) do set "DB_NAME_FROM_URL=%%A"
exit /b 0

:RESOLVE_SQLCMD_SERVER
for /f "usebackq delims=" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$u=$env:DB_URL; if ($u -match '^jdbc:sqlserver://([^;]+)') { $s=$Matches[1]; if ($s -match '^(.*):([0-9]+)$') { $Matches[1] + ',' + $Matches[2] } else { $s } }"`) do set "DB_SERVER_TARGET=%%A"
exit /b 0

:VALIDATE_TARGET
if /i "%DB_NAME%"=="ETL_SISTEMA" (
    echo [ERRO] Este script nunca pode operar no banco ETL_SISTEMA.
    exit /b 1
)
if /i "%DB_NAME%"=="SATELITE_TMS_AUDITORIA" (
    echo [ERRO] Este script nunca pode operar no banco SATELITE_TMS_AUDITORIA.
    exit /b 1
)
if /i "%DB_NAME%"=="master" (
    echo [ERRO] Este script nao pode operar no banco master.
    exit /b 1
)
if /i "%ENVIRONMENT%"=="dev" if /i "%DB_NAME%"=="DASHBOARDS" (
    echo [ERRO] DEV nao pode apontar para DASHBOARDS. Use DASHBOARDS_DEV.
    exit /b 1
)
if /i "%SPRING_PROFILES_ACTIVE%"=="dev" if /i "%DB_NAME%"=="DASHBOARDS" (
    echo [ERRO] Profile dev nao pode apontar para DASHBOARDS. Use DASHBOARDS_DEV.
    exit /b 1
)
if /i not "%DASHBOARDS_DB_ALLOW_CUSTOM%"=="1" (
    if /i not "%DB_NAME%"=="DASHBOARDS" if /i not "%DB_NAME%"=="DASHBOARDS_DEV" (
        echo [ERRO] Database nao prevista: %DB_NAME%
        echo Use DASHBOARDS ou DASHBOARDS_DEV, ou defina DASHBOARDS_DB_ALLOW_CUSTOM=1 para sandbox controlado.
        exit /b 1
    )
)
exit /b 0

:RUN_FLYWAY
set "FLYWAY_GOAL=%~1"
echo [ETAPA] Flyway %FLYWAY_GOAL%...
if "%DB_USER%"=="" (
    call "backend\mvnw.cmd" -f "backend\pom.xml" flyway:%FLYWAY_GOAL% "-Dflyway.url=%DB_URL%" "-Dflyway.locations=filesystem:%DASHBOARD_ROOT%\database\migrations" "-Dflyway.baselineOnMigrate=true" "-Dflyway.baselineVersion=22" "-Dflyway.baselineDescription=Baseline schema existente ate V022" "-Dflyway.validateOnMigrate=true" "-Dflyway.outOfOrder=false" "-Dflyway.encoding=UTF-8"
) else (
    if "%DB_PASSWORD%"=="" (
        echo [ERRO] DB_USER definido mas DB_PASSWORD esta vazio.
        exit /b 1
    )
    call "backend\mvnw.cmd" -f "backend\pom.xml" flyway:%FLYWAY_GOAL% "-Dflyway.url=%DB_URL%" "-Dflyway.user=%DB_USER%" "-Dflyway.password=%DB_PASSWORD%" "-Dflyway.locations=filesystem:%DASHBOARD_ROOT%\database\migrations" "-Dflyway.baselineOnMigrate=true" "-Dflyway.baselineVersion=22" "-Dflyway.baselineDescription=Baseline schema existente ate V022" "-Dflyway.validateOnMigrate=true" "-Dflyway.outOfOrder=false" "-Dflyway.encoding=UTF-8"
)
if errorlevel 1 (
    echo [ERRO] Flyway %FLYWAY_GOAL% falhou.
    exit /b 1
)
echo [OK] Flyway %FLYWAY_GOAL% concluido.
echo.
exit /b 0

:RUN_VALIDATIONS
echo [ETAPA] Validacoes SQL...
if "%DB_SERVER_TARGET%"=="" (
    echo [ERRO] Nao foi possivel resolver servidor para sqlcmd.
    exit /b 1
)
where sqlcmd >nul 2>nul
if errorlevel 1 (
    echo [ERRO] sqlcmd nao encontrado no PATH.
    echo Instale o SQL Server Command Line Utilities ou execute com --no-validation.
    exit /b 1
)

set "SQLCMD_FLAGS=-I -f 65001"
call :RESOLVE_SQLCMD_TRUST_CERT
if "%SQLCMD_TRUST_CERT%"=="1" set "SQLCMD_FLAGS=%SQLCMD_FLAGS% -C"
if not "%SQLCMD_EXTRA_ARGS%"=="" set "SQLCMD_FLAGS=%SQLCMD_FLAGS% %SQLCMD_EXTRA_ARGS%"

if "%DB_USER%"=="" (
    set "AUTH_CMD=-E"
) else (
    if "%DB_PASSWORD%"=="" (
        echo [ERRO] DB_USER definido mas DB_PASSWORD esta vazio.
        exit /b 1
    )
    set "AUTH_CMD=-U %DB_USER%"
    set "SQLCMDPASSWORD=%DB_PASSWORD%"
)

for %%F in (
    "database\validation\001_validar_escopo_filiais_usuario.sql"
    "database\validation\002_validar_metas_custo_manifestos.sql"
) do (
    if exist %%F (
        echo   [EXEC] %%~F
        sqlcmd %SQLCMD_FLAGS% -S "%DB_SERVER_TARGET%" -d "%DB_NAME%" %AUTH_CMD% -i "%%~F" -b
        if errorlevel 1 (
            echo [ERRO] Falha na validacao: %%~F
            set "SQLCMDPASSWORD="
            exit /b 1
        )
    )
)
set "SQLCMDPASSWORD="
echo [OK] Validacoes concluidas.
echo.
exit /b 0

:RESOLVE_SQLCMD_TRUST_CERT
set "SQLCMD_TRUST_CERT=0"
for /f "usebackq delims=" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$extra=$env:SQLCMD_EXTRA_ARGS; if ($extra -match '(^|\s)-C(\s|$)') { '0' } elseif ($env:DB_TRUST_SERVER_CERTIFICATE -match '^(?i:true|1)$') { '1' } elseif ($env:DB_URL -match '(?i)(?:^|;)trustServerCertificate=true(?:;|$)') { '1' } else { '0' }"`) do set "SQLCMD_TRUST_CERT=%%A"
exit /b 0

:MOSTRAR_AJUDA
echo.
echo Uso:
echo   database\executar_database.bat
echo      Aplica migrations pendentes via Flyway e roda validacoes SQL.
echo.
echo   database\executar_database.bat --info
echo      Mostra o estado das migrations sem alterar o banco.
echo.
echo   database\executar_database.bat --validate-only
echo      Roda apenas database\validation\*.sql selecionados.
echo.
echo   database\executar_database.bat --no-validation
echo      Aplica migrations sem sqlcmd.
echo.
echo   database\executar_database.bat --env-file .env
echo      Usa um arquivo .env especifico.
echo.
echo Configuracao:
echo   1. Copie database\config_exemplo.bat para database\config.bat; ou
echo   2. Configure DB_URL, DB_USER e DB_PASSWORD em .env.development.local/.env.
echo.
exit /b 0
