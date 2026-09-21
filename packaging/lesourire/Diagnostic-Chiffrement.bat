@echo off
setlocal EnableExtensions
cd /d "%~dp0"

REM ===========================================================================
REM Diagnostic du chiffrement Le Sourire (lecture seule par defaut).
REM A lancer quand le client affiche des erreurs sur les listes, quand le
REM serveur refuse de demarrer, ou apres une reinstanciation du dossier.
REM
REM Options utiles (transmises telles quelles) :
REM   --afficher-cles                 affiche les cles trouvees en base64
REM   --exporter-cle <fichier>        ecrit la cle qui relit les donnees
REM   --verifier-cle <cle|fichier>    teste une cle (code 0 si tout est lisible)
REM   --rechiffrer <cle|fichier> --je-confirme
REM                                   reecrit la base sous une cle unique
REM
REM Le rapport complet est ecrit dans DIAGNOSTIC-CHIFFREMENT.txt (a cote de ce
REM fichier). Envoyez ce rapport au support ; il ne contient jamais de donnee
REM patient, ni la cle (sauf avec --afficher-cles).
REM ===========================================================================

set "JAVA_HOME=%~dp0jre-windows"
set "SERVEUR_HOME=%~dp0serveur"
set "JAR=%SERVEUR_HOME%\lesourire-serveur.jar"
set "RAPPORT=%~dp0DIAGNOSTIC-CHIFFREMENT.txt"
set "LESOURIRE_HOME=%~dp0"

if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)

if not exist "%JAR%" (
    echo JAR serveur introuvable : %JAR%
    pause
    exit /b 1
)

REM Reglages MariaDB du cabinet (memes valeurs que le serveur).
if exist "%SERVEUR_HOME%\lesourire-serveur.conf.bat" call "%SERVEUR_HOME%\lesourire-serveur.conf.bat"
if not defined LESOURIRE_BD_URL set "LESOURIRE_BD_URL=jdbc:mariadb://localhost:3306/lesourire"
if not defined LESOURIRE_BD_UTILISATEUR set "LESOURIRE_BD_UTILISATEUR=admin"
if not defined LESOURIRE_BD_MOT_DE_PASSE set "LESOURIRE_BD_MOT_DE_PASSE=csa-soft"

echo Diagnostic du chiffrement Le Sourire
echo   Base de donnees : %LESOURIRE_BD_URL%
echo   Rapport         : %RAPPORT%
echo   Sans option, cette analyse est en LECTURE SEULE (aucune donnee modifiee).
echo.

"%JAVA_CMD%" -Dfile.encoding=UTF-8 -Dloader.main=com.lesourire.serveur.outils.DiagnosticChiffrement -cp "%JAR%" org.springframework.boot.loader.launch.PropertiesLauncher --rapport "%RAPPORT%" %*

set "CODE=%ERRORLEVEL%"
echo.
if "%CODE%"=="0" echo Resultat : toutes les donnees sont lisibles avec une cle connue ^(code 0^).
if "%CODE%"=="1" echo Resultat : erreur d'execution ^(code 1^) - lisez le rapport.
if "%CODE%"=="2" echo Resultat : des donnees ne sont relues par AUCUNE cle ^(code 2^).
if "%CODE%"=="3" echo Resultat : rechiffrement partiel ^(code 3^) - des lignes restent illisibles.
echo Rapport complet : %RAPPORT%
echo.
pause
endlocal
exit /b %CODE%