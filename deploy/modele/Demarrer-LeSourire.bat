@echo off
REM Raccourcis a la racine du pack — double-clic.
cd /d "%~dp0"
start "Le Sourire - Serveur" cmd /k "%~dp0serveur\demarrer-serveur.bat"
timeout /t 3 /nobreak >nul
start "Le Sourire - Client" cmd /c "%~dp0client\demarrer-client.bat"
