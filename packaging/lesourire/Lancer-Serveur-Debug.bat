@echo off
setlocal EnableExtensions
cd /d "%~dp0"

REM ===========================================================================
REM Lance le serveur EN MODE VISIBLE : tout s'affiche a l'ecran ET s'ecrit dans
REM serveur\logs\lesourire-serveur.log. A utiliser des qu'un demarrage echoue :
REM le message d'erreur reste affiche au lieu de disparaitre avec la fenetre.
REM
REM Aucun PowerShell, aucun chemin passe en argument : ce fichier fonctionne
REM meme si le chemin du dossier contient des espaces.
REM Ctrl+C pour arreter le serveur.
REM ===========================================================================

set "JAVA_HOME=%~dp0jre-windows"
set "SERVEUR_HOME=%~dp0serveur"
set "JAR=%SERVEUR_HOME%\lesourire-serveur.jar"
set "LESOURIRE_HOME=%~dp0"
set "LESOURIRE_LOG=%SERVEUR_HOME%\logs\lesourire-serveur.log"

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

if not exist "%SERVEUR_HOME%\logs" mkdir "%SERVEUR_HOME%\logs"
if exist "%SERVEUR_HOME%\lesourire-serveur.conf.bat" call "%SERVEUR_HOME%\lesourire-serveur.conf.bat"

echo Demarrage du serveur Le Sourire EN MODE VISIBLE
echo   Dossier : %~dp0
echo   Journal : %LESOURIRE_LOG%
echo   Cle     : %LESOURIRE_HOME%fichiers\cle-chiffrement.key
if exist "%LESOURIRE_HOME%fichiers\EMPREINTE.txt" type "%LESOURIRE_HOME%fichiers\EMPREINTE.txt"
echo.
echo Ctrl+C pour arreter le serveur.
echo.

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar "%JAR%"

echo.
echo === Le serveur s'est arrete. Copiez le texte ci-dessus pour le support ===
echo === (clic droit dans cette fenetre, Selectionner tout, Entree) ===
pause
endlocal
exit /b 0