@echo off
REM ============================================================
REM Arquivo : database/config_exemplo.bat
REM Papel   : Modelo local para database/executar_database.bat.
REM
REM INSTRUCOES:
REM   1. Copie para database/config.bat.
REM   2. Ajuste DB_URL, DB_USER e DB_PASSWORD.
REM   3. Nunca commite database/config.bat.
REM ============================================================

REM DEV seguro. Para producao, aponte explicitamente para DASHBOARDS
REM e rode em uma janela operacional controlada.
set DB_URL=jdbc:sqlserver://localhost:1433;databaseName=DASHBOARDS_DEV;encrypt=true;trustServerCertificate=true
set DB_USER=usuario_dashboard
set DB_PASSWORD=sua_senha

REM Usado pelas validacoes via sqlcmd. Se vazio, o executor tenta derivar do DB_URL.
set DB_SERVER=
set DB_PORT=
set DB_NAME=

REM Flags extras do sqlcmd. O executor ja deriva -C de trustServerCertificate=true no DB_URL.
set SQLCMD_EXTRA_ARGS=

REM Permite banco customizado de sandbox diferente de DASHBOARDS/DASHBOARDS_DEV.
set DASHBOARDS_DB_ALLOW_CUSTOM=0
