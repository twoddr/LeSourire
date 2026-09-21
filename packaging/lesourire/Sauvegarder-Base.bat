@echo off
setlocal EnableExtensions
cd /d "%~dp0"

REM ===========================================================================
REM Sauvegarde de la base MariaDB SANS passer par le client : double-clic.
REM Le dump (.sql) ET la cle de chiffrement (cle-chiffrement.key) sont ecrits
REM dans le dossier "sauvegardes" du paquet : les deux vont toujours ensemble.
REM
REM Options possibles (a taper dans une invite de commandes) :
REM   --dossier "D:\mes sauvegardes"   autre destination
REM   --mysqldump "C:\...\mysqldump.exe"   chemin explicite (sinon recherche auto)
REM ===========================================================================

set "JAVA_HOME=%~dp0jre-windows"
set "SERVEUR_HOME=%~dp0serveur"
set "JAR=%SERVEUR_HOME%\lesourire-serveur.jar"
set "LESOURIRE_HOME=%~dp0"
set "DOSSIER=%~dp0sauvegardes"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java Runtime introuvable : jre-windows\
    pause
    exit /b 1
)
if not exist "%JAR%" (
    echo JAR serveur introuvable : %JAR%
    pause
    exit /b 1
)
if exist "%SERVEUR_HOME%\lesourire-serveur.conf.bat" call "%SERVEUR_HOME%\lesourire-serveur.conf.bat"

echo Sauvegarde de la base Le Sourire ^(lecture seule^)
echo   Destination : %DOSSIER%
if exist "%LESOURIRE_HOME%fichiers\EMPREINTE.txt" type "%LESOURIRE_HOME%fichiers\EMPREINTE.txt"
echo.

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -Dloader.main=com.lesourire.serveur.outils.SauvegardeBase -cp "%JAR%" org.springframework.boot.loader.launch.PropertiesLauncher --dossier "%DOSSIER%" %*

set "CODE=%ERRORLEVEL%"
echo.
if "%CODE%"=="0" echo Sauvegarde terminee. Le fichier .sql et la cle sont dans :
if "%CODE%"=="0" echo   %DOSSIER%
if not "%CODE%"=="0" echo ECHEC de la sauvegarde ^(code %CODE%^) : lisez le message ci-dessus.
pause
endlocal
exit /b %CODE%