@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

REM Port : valeur par defaut, puis surcharge par la configuration du serveur.
REM IMPORTANT : ne jamais mettre le "call" et la lecture de LESOURIRE_PORT
REM dans un meme bloc "if ( ... )" : cmd.exe y developperait la variable du
REM port AVANT d'executer le "call", donc avec la valeur precedente (vide) :
REM le port resterait vide et l'attente du serveur echouerait toujours.
set "PORT=8420"
set "CONF_SERVEUR=%~dp0serveur\lesourire-serveur.conf.bat"
if exist "%CONF_SERVEUR%" call "%CONF_SERVEUR%"
if defined LESOURIRE_PORT set "PORT=%LESOURIRE_PORT%"
if not defined PORT set "PORT=8420"

echo Lancement du serveur Le Sourire ^(fenetre masquee^)...
call "%~dp01-Demarrer-Serveur.bat"
if errorlevel 1 (
    echo.
    echo Le serveur n'a pas pu demarrer.
    echo Consultez serveur\logs\lesourire-serveur.log.
    pause
    exit /b 1
)

echo Attente du serveur ^(http://127.0.0.1:!PORT!/api/systeme/statut^) ...
echo   Le client ne se lancera qu'une fois le serveur pret a repondre.
echo.

REM PID du serveur lance par 1-Demarrer-Serveur.bat : il permet de renoncer
REM rapidement si le processus disparait (au lieu d'attendre 3 minutes).
set "PID_FILE=%~dp0serveur\lesourire-serveur.pid"
set "LOG_FILE=%~dp0serveur\logs\lesourire-serveur.log"
set "SERVEUR_PID="
if exist "%PID_FILE%" set /p SERVEUR_PID=<"%PID_FILE%"

set "n=0"
:attente
set /a n+=1
if !n! gtr 180 goto echec

REM Toutes les 5 secondes, verifier que le processus du serveur est toujours
REM vivant : s'il a disparu (MariaDB arrete, identifiants refuses, port deja
REM occupe...), on renonce tout de suite au lieu de patienter 3 minutes.
REM La recherche de l'identifiant via find est independante de la langue de Windows.
set /a test_pid=!n! %% 5
if not !test_pid!==1 goto test_dispo
if not defined SERVEUR_PID goto test_dispo
tasklist /FI "PID eq !SERVEUR_PID!" /NH 2>nul | find "!SERVEUR_PID!" >nul
if errorlevel 1 goto serveur_mort

:test_dispo
REM Test de disponibilite : curl.exe (fourni avec Windows 10/11) sinon PowerShell.
set "pret=0"
where curl.exe >nul 2>&1
if not errorlevel 1 (
    curl.exe -sf --connect-timeout 2 --max-time 3 "http://127.0.0.1:!PORT!/api/systeme/statut" >nul 2>&1
    if not errorlevel 1 set "pret=1"
) else (
    powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:!PORT!/api/systeme/statut' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
    if not errorlevel 1 set "pret=1"
)

if "!pret!"=="1" goto pret

set /a aff=!n! %% 5
if !aff!==0 echo   ... demarrage du serveur en cours ^(!n! s^)
REM Pause d'une seconde, fiable meme sans console (contrairement a timeout).
ping -n 2 127.0.0.1 >nul
goto attente

:pret
echo Serveur pret. Lancement du client...
call "%~dp02-Demarrer-LeSourire.bat"
endlocal
exit /b 0

:serveur_mort
echo.
echo Le processus du serveur ^(PID !SERVEUR_PID!^) s'est arrete pendant le demarrage.
echo Causes les plus frequentes :
echo   - MariaDB ^(ou MySQL^) n'est pas demarre sur ce poste ;
echo   - URL ou identifiants incorrects dans serveur\lesourire-serveur.conf.bat ;
echo   - le port !PORT! est deja utilise par une autre application.
echo.
echo Dernieres lignes du journal ^(serveur\logs\lesourire-serveur.log^) :
echo ---------------------------------------------------------------
if exist "!LOG_FILE!" (
    powershell -NoProfile -Command "Get-Content -LiteralPath '!LOG_FILE!' -Tail 12" 2>nul
) else (
    echo ^(journal introuvable : !LOG_FILE!^)
)
echo ---------------------------------------------------------------
echo.
echo Corrigez la cause ci-dessus, puis relancez ToutDemarrer.bat
echo ^(ou lancez 1-Demarrer-Serveur.bat puis 2-Demarrer-LeSourire.bat^).
pause
endlocal
exit /b 1

:echec
echo.
echo Le serveur ne repond pas apres 3 minutes.
echo Verifiez MariaDB, serveur\lesourire-serveur.conf.bat,
echo et les logs : serveur\logs\lesourire-serveur.log
echo ^(ou lancez 2-Demarrer-LeSourire.bat manuellement^).
pause
endlocal
exit /b 1
