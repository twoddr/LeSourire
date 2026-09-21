#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home

SERVEUR_HOME="${APP_HOME}/serveur"
JAR="${SERVEUR_HOME}/lesourire-serveur.jar"
LOG_DIR="${SERVEUR_HOME}/logs"
LOG_FILE="${LOG_DIR}/lesourire-serveur.log"
PID_FILE="${SERVEUR_HOME}/lesourire-serveur.pid"

# Racine du paquet : permet de retrouver fichiers/cle-chiffrement.key quel que
# soit le dossier depuis lequel le serveur est lancé (raccourci, service, cron).
# Sans cela, une nouvelle clé pourrait être générée et les données déjà
# chiffrées deviendraient illisibles.
export LESOURIRE_HOME="${APP_HOME}"
FICHIER_CLE="${APP_HOME}/fichiers/cle-chiffrement.key"
JAVA_OPTIONS=(-Dfile.encoding=UTF-8 "-Dlesourire.home=${APP_HOME}")

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

mkdir -p "${LOG_DIR}"

echo "Le client ne doit être lancé qu'une fois le serveur prêt à répondre."
echo "Clé de chiffrement : ${FICHIER_CLE}"
if [ ! -f "${FICHIER_CLE}" ]; then
    echo "ATTENTION : aucune clé de chiffrement à cet emplacement."
    echo "  Si cette installation contient déjà des patients, ARRÊTEZ-VOUS et"
    echo "  restaurez la clé d'origine (ou lancez Diagnostic-Chiffrement.sh)."
fi

if [ "${LESOURIRE_SERVEUR_FOND:-0}" = "1" ]; then
    # Mode arrière-plan (utilisé par ToutDemarrer.sh) : console invisible,
    # logs dans un fichier, PID mémorisé pour Arreter-Serveur.sh.
    nohup "${JAVA_HOME}/bin/java" "${JAVA_OPTIONS[@]}" -jar "${JAR}" \
        >> "${LOG_FILE}" 2>&1 &
    PID=$!
    echo "${PID}" > "${PID_FILE}"
    echo "Serveur lancé en arrière-plan (PID ${PID})."
    echo "Logs : ${LOG_FILE}"
    echo "Arrêt : ${APP_HOME}/Arreter-Serveur.sh"
    exit 0
fi

echo "Démarrage du serveur Le Sourire sur le port ${LESOURIRE_PORT:-8420} ..."
echo "Logs : ${LOG_FILE}"
echo "Ne fermez pas ce terminal (Ctrl+C pour arrêter)."
echo

"${JAVA_HOME}/bin/java" "${JAVA_OPTIONS[@]}" -jar "${JAR}" 2>&1 | tee -a "${LOG_FILE}"
