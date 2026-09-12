#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# Port (conf serveur si présente)
if [ -f "${APP_HOME}/serveur/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${APP_HOME}/serveur/lesourire-serveur.conf.sh"
fi
PORT="${LESOURIRE_PORT:-8420}"

echo "Lancement du serveur Le Sourire (arrière-plan)..."
LESOURIRE_SERVEUR_FOND=1 "${APP_HOME}/1-Demarrer-Serveur.sh"

echo "Attente du serveur (http://127.0.0.1:${PORT}/api/systeme/statut) ..."
for i in $(seq 1 180); do
    if curl -sf --connect-timeout 2 --max-time 3 \
        "http://127.0.0.1:${PORT}/api/systeme/statut" >/dev/null 2>&1; then
        echo "Serveur prêt. Lancement du client..."
        "${APP_HOME}/2-Demarrer-LeSourire.sh"
        exit 0
    fi
    sleep 1
done

echo
echo "Le serveur ne répond pas après 3 minutes."
echo "Vérifiez MariaDB, les identifiants dans serveur/lesourire-serveur.conf.sh,"
echo "et les logs : serveur/logs/lesourire-serveur.log"
echo "(ou lancez ./2-Demarrer-LeSourire.sh manuellement)."
exit 1
