@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "PID_FILE=%~dp0serveur\lesourire-serveur.pid"

if not exist "%PID_FILE%" (
    echo Aucun serveur Le Sourire n'a ete lance en arriere-plan.
    echo ^(fichier %PID_FILE% introuvable^)
    pause
    exit /b 1
)

set "SERVEUR_PID="
set /p SERVEUR_PID=<"%PID_FILE%"

if not defined SERVEUR_PID (
    echo Le fichier %PID_FILE% est vide.
    pause
    exit /b 1
)

echo Arret du serveur Le Sourire ^(PID %SERVEUR_PID%^)...
taskkill /PID %SERVEUR_PID% /F >nul 2>&1
if errorlevel 1 (
    echo Le serveur n'a pas pu etre arrete ^(il est peut-etre deja ferme^).
) else (
    echo Serveur arrete.
)

del "%PID_FILE%" >nul 2>&1
pause
endlocal
exit /b 0