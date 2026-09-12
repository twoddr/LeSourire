#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="${APP_HOME}/serveur/lesourire-serveur.pid"

if [ ! -f "${PID_FILE}" ]; then
    echo "Aucun serveur Le Sourire n'a été lancé en arrière-plan"
    echo "(fichier ${PID_FILE} introuvable)."
    exit 1
fi

PID="$(tr -d '[:space:]' < "${PID_FILE}")"
if [ -z "${PID}" ] || ! kill -0 "${PID}" 2>/dev/null; then
    echo "Le serveur (PID ${PID:-inconnu}) n'est plus actif."
    rm -f "${PID_FILE}"
    exit 0
fi

echo "Arrêt du serveur Le Sourire (PID ${PID})..."
kill "${PID}"
rm -f "${PID_FILE}"
echo "Serveur arrêté."