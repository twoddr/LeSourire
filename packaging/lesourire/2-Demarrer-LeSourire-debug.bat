@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=%~dp0jre-windows"
set "CLIENT_HOME=%~dp0client"
set "JFX_DIR=%CLIENT_HOME%\javafx-windows"
set "MODULE_PATH=%CLIENT_HOME%;%CLIENT_HOME%\lib;%JFX_DIR%"

echo JAVA_HOME  : %JAVA_HOME%
echo MODULE_PATH: %MODULE_PATH%
echo.

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java Runtime introuvable.
    pause
    exit /b 1
)

echo Lancement du client Le Sourire (mode debug)...
echo.

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 --module-path "%MODULE_PATH%" --module com.lesourire.client/com.lesourire.client.LeSourireClient
echo.
echo Code sortie : %ERRORLEVEL%
pause
endlocal
