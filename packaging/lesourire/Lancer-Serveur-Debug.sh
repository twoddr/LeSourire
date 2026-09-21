#!/bin/bash
# ============================================================================
# Lance le serveur EN MODE VISIBLE : tout s'affiche dans le terminal ET s'ecrit
# dans serveur/logs/lesourire-serveur.log. A utiliser des qu'un demarrage
# echoue : le message d'erreur reste affiche au lieu de disparaitre.
# Ctrl+C pour arreter le serveur.
# ============================================================================
set -uo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home

SERVEUR_HOME="${APP_HOME}/serveur"
JAR="${SERVEUR_HOME}/lesourire-serveur.jar"
export LESOURIRE_HOME="${APP_HOME}"
export LESOURIRE_LOG="${SERVEUR_HOME}/logs/lesourire-serveur.log"

if [ ! -f "${JAR}" ]; then
    echo "JAR serveur introuvable : ${JAR}"
    exit 1
fi
mkdir -p "${SERVEUR_HOME}/logs"
if [ -f "${SERVEUR_HOME}/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${SERVEUR_HOME}/lesourire-serveur.conf.sh"
fi

echo "Demarrage du serveur Le Sourire EN MODE VISIBLE"
echo "  Dossier : ${APP_HOME}"
echo "  Journal : ${LESOURIRE_LOG}"
echo "  Cle     : ${APP_HOME}/fichiers/cle-chiffrement.key"
if [ -f "${APP_HOME}/fichiers/EMPREINTE.txt" ]; then
    cat "${APP_HOME}/fichiers/EMPREINTE.txt"
fi
echo
echo "Ctrl+C pour arreter le serveur."
echo

"${JAVA_HOME}/bin/java" -Dfile.encoding=UTF-8 -jar "${JAR}"
CODE=$?

echo
echo "Le serveur s'est arrete (code ${CODE})."
exit "${CODE}"