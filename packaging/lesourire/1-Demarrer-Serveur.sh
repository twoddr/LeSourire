#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home

SERVEUR_HOME="${APP_HOME}/serveur"
JAR="${SERVEUR_HOME}/lesourire-serveur.jar"

if [ ! -x "${JAVA_HOME}/bin/java" ]; then
    echo "Java Runtime introuvable (jre-${LESOURIRE_OS}/)."
    echo "Copiez une JRE 21 dans ce dossier (voir JRE-A-COPIER.txt)."
    exit 1
fi

if [ ! -f "${JAR}" ]; then
    echo "JAR serveur introuvable : ${JAR}"
    exit 1
fi

if [ -f "${SERVEUR_HOME}/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${SERVEUR_HOME}/lesourire-serveur.conf.sh"
fi

echo "Démarrage du serveur Le Sourire sur le port ${LESOURIRE_PORT:-8420} ..."
echo "Ne fermez pas ce terminal."
echo

exec "${JAVA_HOME}/bin/java" -Dfile.encoding=UTF-8 -jar "${JAR}"
