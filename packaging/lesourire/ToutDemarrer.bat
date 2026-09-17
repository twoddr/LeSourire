@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "PORT=8420"
if exist "%~dp0serveur\lesourire-serveur.conf.bat" (
    call "%~dp0serveur\lesourire-serveur.conf.bat"
    if defined LESOURIRE_PORT set "PORT=%LESOURIRE_PORT%"
)

echo Lancement du serveur Le Sourire ^(fenêtre masquée^)...
call "%~dp01-Demarrer-Serveur.bat"
if errorlevel 1 (
    echo.
    echo Le serveur n'a pas pu démarrer.
    echo Consultez serveur\logs\lesourire-serveur.log.
    pause
    exit /b 1
)

echo Attente du serveur ^(http://127.0.0.1:%PORT%/api/systeme/statut^) ...
echo   Le client ne se lancera qu'une fois le serveur pret a repondre.
echo.

set "n=0"
:attente
set /a n+=1
if !n! gtr 180 goto echec

REM Test de disponibilité : curl.exe (fourni avec Windows 10/11) sinon PowerShell.
set "pret=0"
where curl.exe >nul 2>&1
if not errorlevel 1 (
    curl.exe -sf --connect-timeout 2 --max-time 3 "http://127.0.0.1:%PORT%/api/systeme/statut" >nul 2>&1
    if not errorlevel 1 set "pret=1"
) else (
    powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:%PORT%/api/systeme/statut' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
    if not errorlevel 1 set "pret=1"
)

if "!pret!"=="1" goto pret

set /a aff=!n! %% 5
if !aff!==0 echo   ... demarrage du serveur en cours ^(!n! s^)
REM Pause d'une seconde, fiable même sans console (contrairement à timeout).
ping -n 2 127.0.0.1 >nul
goto attente

:pret
echo Serveur prêt. Lancement du client...
call "%~dp02-Demarrer-LeSourire.bat"
endlocal
exit /b 0

:echec
echo.
echo Le serveur ne répond pas après 3 minutes.
echo Vérifiez MariaDB, serveur\lesourire-serveur.conf.bat,
echo et les logs : serveur\logs\lesourire-serveur.log
echo ^(ou lancez 2-Demarrer-LeSourire.bat manuellement^).
pause
endlocal
exit /b 1
