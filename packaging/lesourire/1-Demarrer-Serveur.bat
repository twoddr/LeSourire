@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=%~dp0jre-windows"
set "SERVEUR_HOME=%~dp0serveur"
set "JAR=%SERVEUR_HOME%\lesourire-serveur.jar"
set "LOG_DIR=%SERVEUR_HOME%\logs"
set "LOG_FILE=%LOG_DIR%\lesourire-serveur.log"
set "PID_FILE=%SERVEUR_HOME%\lesourire-serveur.pid"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java Runtime introuvable : jre-windows\
    echo Copiez une JRE 21 Windows dans ce dossier ^(voir JRE-A-COPIER.txt^).
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo JAR serveur introuvable : %JAR%
    pause
    exit /b 1
)

if exist "%SERVEUR_HOME%\lesourire-serveur.conf.bat" (
    call "%SERVEUR_HOME%\lesourire-serveur.conf.bat"
)
if not defined LESOURIRE_PORT set "LESOURIRE_PORT=8420"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM Console masquée : on préfère javaw.exe ; les logs Spring Boot sont écrits
REM dans serveur\logs\lesourire-serveur.log (variable LESOURIRE_LOG).
set "LESOURIRE_LOG=%LOG_FILE%"

set "JAVA_CMD="
if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_CMD=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA_CMD set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

echo Démarrage du serveur Le Sourire en arrière-plan...
echo   Port   : %LESOURIRE_PORT%
echo   Logs   : %LOG_FILE%
echo   Arrêt  : Arreter-Serveur.bat
echo   Le client ne doit être lancé qu'une fois le serveur prêt à répondre.
echo.

REM Lance le serveur sans fenêtre et mémorise son PID pour Arreter-Serveur.bat.
REM Le répertoire de travail est serveur\ : le nom du JAR est ainsi sans espaces.
powershell -NoProfile -Command "$p = Start-Process -FilePath '%JAVA_CMD%' -ArgumentList '-Dfile.encoding=UTF-8','-jar','lesourire-serveur.jar' -WorkingDirectory '%SERVEUR_HOME%' -WindowStyle Hidden -PassThru; [System.IO.File]::WriteAllText('%PID_FILE%', [string]$p.Id)"
if errorlevel 1 (
    echo Impossible de lancer le serveur. Consultez le terminal.
    pause
    exit /b 1
)

set "SERVEUR_PID="
set /p SERVEUR_PID=<"%PID_FILE%"
if defined SERVEUR_PID echo Serveur lancé en arrière-plan ^(PID %SERVEUR_PID%^).

endlocal
exit /b 0
