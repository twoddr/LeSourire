@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=%~dp0jre-windows"
set "CLIENT_HOME=%~dp0client"
set "JFX_DIR=%CLIENT_HOME%\javafx-windows"
set "LOG_DIR=%CLIENT_HOME%\logs"
set "LOG_FILE=%LOG_DIR%\lesourire-client.log"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java Runtime introuvable : jre-windows\
    echo Copiez une JRE 21 Windows dans ce dossier ^(voir JRE-A-COPIER.txt^).
    pause
    exit /b 1
)

if not exist "%CLIENT_HOME%\lesourire-client.jar" (
    echo Client introuvable : %CLIENT_HOME%\lesourire-client.jar
    pause
    exit /b 1
)

if not exist "%JFX_DIR%" (
    echo Dossier JavaFX manquant : %JFX_DIR%
    pause
    exit /b 1
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "MODULE_PATH=%CLIENT_HOME%;%CLIENT_HOME%\lib;%JFX_DIR%"

REM Fenêtre masquée : on préfère javaw.exe (aucune console). La sortie est
REM journalisée dans client\logs\lesourire-client.log ; pour un lancement avec
REM console, utilisez 2-Demarrer-LeSourire-debug.bat.
set "JAVA_CMD="
if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_CMD=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA_CMD set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

"%JAVA_CMD%" -Dfile.encoding=UTF-8 --module-path "%MODULE_PATH%" --module com.lesourire.client/com.lesourire.client.LeSourireClient > "%LOG_FILE%" 2>&1
endlocal
