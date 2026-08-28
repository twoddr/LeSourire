@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=%~dp0jre-windows"
set "SERVEUR_HOME=%~dp0serveur"
set "JAR=%SERVEUR_HOME%\lesourire-serveur.jar"

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

echo Demarrage du serveur Le Sourire sur le port %LESOURIRE_PORT% ...
echo Ne fermez pas cette fenetre.
echo.

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar "%JAR%"
if errorlevel 1 pause
endlocal
