@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=%~dp0jre-windows"
set "SERVEUR_HOME=%~dp0serveur"
set "JAR=%SERVEUR_HOME%\lesourire-serveur.jar"
set "LOG_DIR=%SERVEUR_HOME%\logs"
set "LOG_FILE=%LOG_DIR%\lesourire-serveur.log"
set "PID_FILE=%SERVEUR_HOME%\lesourire-serveur.pid"

REM Racine du paquet : permet au serveur de retrouver fichiers\cle-chiffrement.key
REM quel que soit le dossier depuis lequel il est lance (menu Demarrer, bureau,
REM tache planifiee, ancien raccourci...). Sans cela, une cle differente pourrait
REM etre generee et les donnees deja chiffrees deviendraient illisibles.
set "LESOURIRE_HOME=%~dp0"
set "FICHIER_CLE=%LESOURIRE_HOME%fichiers\cle-chiffrement.key"

REM Version du paquet (lue dans BUILD-INFO.txt) : evite de confondre deux dossiers.
set "VERSION_PAQUET="
if exist "%~dp0BUILD-INFO.txt" for /f "tokens=2 delims==" %%v in ('findstr /b /c:"version=" "%~dp0BUILD-INFO.txt"') do set "VERSION_PAQUET=%%v"

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

REM Console masquee : on prefere javaw.exe ; les logs Spring Boot sont ecrits
REM dans serveur\logs\lesourire-serveur.log (variable LESOURIRE_LOG).
set "LESOURIRE_LOG=%LOG_FILE%"

set "JAVA_CMD="
if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_CMD=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA_CMD set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

echo Demarrage du serveur Le Sourire en arriere-plan...
echo   Dossier : %~dp0
if defined VERSION_PAQUET echo   Version : %VERSION_PAQUET%
if not defined VERSION_PAQUET echo   Version : inconnue ^(BUILD-INFO.txt absent^)
echo   Port    : %LESOURIRE_PORT%
echo   Journal : %LOG_FILE%
echo   Cle     : %FICHIER_CLE%
if exist "%LESOURIRE_HOME%fichiers\EMPREINTE.txt" type "%LESOURIRE_HOME%fichiers\EMPREINTE.txt"
echo   Arret  : Arreter-Serveur.bat
echo   Le client ne doit etre lance qu'une fois le serveur pret a repondre.
echo.

REM Signale tot une cle absente : sans elle le serveur genererait une nouvelle
REM cle (donnees deja chiffrees illisibles) et refuse desormais de demarrer.
if not exist "%FICHIER_CLE%" (
    echo ATTENTION : aucune cle de chiffrement dans %FICHIER_CLE%
    echo            Si cette installation contient deja des patients, ARRETEZ-VOUS
    echo            et restaurez la cle d'origine ^(ou lancez Diagnostic-Chiffrement.bat^).
    echo.
)

REM Lance le serveur sans fenetre et memorise son PID pour Arreter-Serveur.bat.
REM Le repertoire de travail est serveur\ : le nom du JAR est ainsi sans espaces.
REM IMPORTANT : ne JAMAIS placer ici un argument contenant %~dp0 (ou tout chemin
REM pouvant contenir un espace). PowerShell recolle les elements de -ArgumentList
REM sans ajouter de guillemets : l'argument serait coupe en deux, Java chercherait
REM une classe inexistante et mourrait SANS aucun message (fenetre masquee, journal
REM meme pas cree). La racine du paquet est transmise par la variable LESOURIRE_HOME
REM ci-dessus, elle, insensible aux espaces et aux renommages de dossier.
powershell -NoProfile -Command "$p = Start-Process -FilePath '%JAVA_CMD%' -ArgumentList '-Dfile.encoding=UTF-8','-jar','lesourire-serveur.jar' -WorkingDirectory '%SERVEUR_HOME%' -WindowStyle Hidden -PassThru; [System.IO.File]::WriteAllText('%PID_FILE%', [string]$p.Id)"
if errorlevel 1 (
    echo Impossible de lancer le serveur. Consultez le terminal.
    pause
    exit /b 1
)

set "SERVEUR_PID="
set /p SERVEUR_PID=<"%PID_FILE%"
if defined SERVEUR_PID echo Serveur lance en arriere-plan ^(PID %SERVEUR_PID%^).

endlocal
exit /b 0
