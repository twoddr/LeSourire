@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "PORT=8420"
if exist "%~dp0serveur\lesourire-serveur.conf.bat" (
    call "%~dp0serveur\lesourire-serveur.conf.bat"
    if defined LESOURIRE_PORT set "PORT=%LESOURIRE_PORT%"
)

echo Lancement du serveur Le Sourire...
start "Le Sourire - Serveur" cmd /c "%~dp01-Demarrer-Serveur.bat"

echo Attente du serveur ^(http://127.0.0.1:%PORT%/api/systeme/statut^) ...
set /a n=0
:wait
set /a n+=1
if %n% gtr 90 goto timeout
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:%PORT%/api/systeme/statut' -UseBasicParsing -TimeoutSec 2; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if %ERRORLEVEL%==0 goto ready
timeout /t 1 /nobreak >nul
goto wait

:ready
echo Serveur pret. Lancement du client...
call "%~dp02-Demarrer-LeSourire.bat"
endlocal
exit /b 0

:timeout
echo.
echo Le serveur ne repond pas apres 90 secondes.
echo Verifiez MariaDB et serveur\lesourire-serveur.conf.bat,
echo puis lancez 2-Demarrer-LeSourire.bat manuellement.
pause
endlocal
exit /b 1
