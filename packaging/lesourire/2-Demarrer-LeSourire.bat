@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=%~dp0jre-windows"
set "CLIENT_HOME=%~dp0client"
set "JFX_DIR=%CLIENT_HOME%\javafx-windows"

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

set "MODULE_PATH=%CLIENT_HOME%;%CLIENT_HOME%\lib;%JFX_DIR%"

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 --module-path "%MODULE_PATH%" --module com.lesourire.client/com.lesourire.client.LeSourireClient
if errorlevel 1 pause
endlocal
