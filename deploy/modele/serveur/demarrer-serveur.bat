@echo off
setlocal
cd /d "%~dp0"

call "%~dp0config.bat"

set "JAVA=%~dp0..\java\bin\java.exe"
if not exist "%JAVA%" (
  where java >nul 2>&1
  if errorlevel 1 (
    echo [ERREUR] Java introuvable.
    echo Placez un JDK/JRE 21 dans le dossier LeSourire\java\
    echo   ^(ex. copier votre dossier java-21 vers LeSourire\java^)
    pause
    exit /b 1
  )
  set "JAVA=java"
)

if not exist "%~dp0lesourire-serveur.jar" (
  echo [ERREUR] lesourire-serveur.jar manquant dans ce dossier.
  pause
  exit /b 1
)

echo.
echo  Le Sourire — Serveur
echo  Port : %LESOURIRE_PORT%
echo  BD   : %LESOURIRE_BD_URL%
echo  Java : %JAVA%
echo.
echo  Laissez cette fenetre ouverte. Ctrl+C pour arreter.
echo.

"%JAVA%" -jar "%~dp0lesourire-serveur.jar"
set ERR=%ERRORLEVEL%
if not "%ERR%"=="0" (
  echo.
  echo [ERREUR] Le serveur s'est arrete ^(code %ERR%^).
  echo Verifiez MariaDB, les identifiants dans config.bat, et le port %LESOURIRE_PORT%.
  pause
)
exit /b %ERR%
