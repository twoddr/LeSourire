@echo off
setlocal
cd /d "%~dp0"

set "JAVA=%~dp0..\java\bin\java.exe"
if not exist "%JAVA%" (
  where java >nul 2>&1
  if errorlevel 1 (
    echo [ERREUR] Java introuvable.
    echo Placez un JDK/JRE 21 dans le dossier LeSourire\java\
    pause
    exit /b 1
  )
  set "JAVA=java"
)

if not exist "%~dp0modules" (
  echo [ERREUR] Dossier modules\ manquant.
  pause
  exit /b 1
)

echo.
echo  Le Sourire — Client
echo  Java : %JAVA%
echo  Serveur par defaut : http://localhost:8420
echo  ^(modifiable dans l'ecran de connexion^)
echo.

"%JAVA%" --module-path "%~dp0modules" -m com.lesourire.client/com.lesourire.client.LeSourireClient
set ERR=%ERRORLEVEL%
if not "%ERR%"=="0" (
  echo.
  echo [ERREUR] Le client s'est arrete ^(code %ERR%^).
  echo Verifiez que le serveur tourne, et que JavaFX est present dans modules\.
  pause
)
exit /b %ERR%
