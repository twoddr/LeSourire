#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# Port (conf serveur si présente)
if [ -f "${APP_HOME}/serveur/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${APP_HOME}/serveur/lesourire-serveur.conf.sh"
fi
PORT="${LESOURIRE_PORT:-8420}"

echo "Lancement du serveur Le Sourire..."
"${APP_HOME}/1-Demarrer-Serveur.sh" &
SERVEUR_PID=$!

cleanup() {
    if kill -0 "${SERVEUR_PID}" 2>/dev/null; then
        kill "${SERVEUR_PID}" 2>/dev/null || true
    fi
}
trap cleanup EXIT

echo "Attente du serveur (http://127.0.0.1:${PORT}/api/systeme/statut) ..."
for i in $(seq 1 90); do
    if curl -sf --connect-timeout 2 --max-time 3 \
        "http://127.0.0.1:${PORT}/api/systeme/statut" >/dev/null 2>&1; then
        echo "Serveur prêt. Lancement du client..."
        "${APP_HOME}/2-Demarrer-LeSourire.sh"
        exit 0
    fi
    sleep 1
done

echo
echo "Le serveur ne répond pas après 90 secondes."
echo "Vérifiez MariaDB, les identifiants dans serveur/lesourire-serveur.conf.sh,"
echo "puis lancez ./2-Demarrer-LeSourire.sh manuellement."
exit 1
