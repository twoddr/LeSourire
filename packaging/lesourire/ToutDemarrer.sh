#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"

# Lancé par double-clic (sans terminal) ? On rouvre une console afin de rendre
# visibles l'indicateur de progression et les journaux.
lesourire_reouvrir_terminal "${APP_HOME}/ToutDemarrer.sh"

# Port (conf serveur si présente)
if [ -f "${APP_HOME}/serveur/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${APP_HOME}/serveur/lesourire-serveur.conf.sh"
fi
PORT="${LESOURIRE_PORT:-8420}"

# Toujours arrêter l'indicateur, même en cas d'échec inattendu.
trap 'lesourire_indicateur_fin' EXIT

echo "Lancement du serveur Le Sourire (arrière-plan)..."
LESOURIRE_SERVEUR_FOND=1 "${APP_HOME}/1-Demarrer-Serveur.sh"

# IMPORTANT : le client ne doit démarrer que lorsque le serveur a fini son
# démarrage (MariaDB, migrations Flyway, contexte Spring). On attend donc que
# /api/systeme/statut réponde — ce point est public et n'accepte des requêtes
# qu'une fois le serveur prêt.
echo "Attente du serveur (http://127.0.0.1:${PORT}/api/systeme/statut) ..."
lesourire_indicateur_debut "Démarrage du serveur en cours…"

pret=0
sans_outil=0
code=0
for i in $(seq 1 180); do
    if lesourire_serveur_pret "${PORT}"; then
        pret=1
        break
    else
        code=$?
    fi
    if [ "${code}" -eq 2 ]; then
        sans_outil=1
        break
    fi
    sleep 1
done
lesourire_indicateur_fin

if [ "${sans_outil}" -eq 1 ]; then
    echo
    echo "Impossible de vérifier le démarrage du serveur : ni 'curl' ni 'nc'"
    echo "n'est disponible sur ce poste. Par précaution, le client n'est pas lancé."
    echo "Installez 'curl' (recommandé) puis relancez ToutDemarrer."
    exit 1
fi

if [ "${pret}" -ne 1 ]; then
    echo
    echo "Le serveur ne répond pas après 3 minutes."
    echo "Vérifiez MariaDB, les identifiants dans serveur/lesourire-serveur.conf.sh,"
    echo "et les logs : serveur/logs/lesourire-serveur.log"
    echo "(ou lancez ./2-Demarrer-LeSourire.sh manuellement)."
    exit 1
fi

echo "Serveur prêt. Lancement du client..."
"${APP_HOME}/2-Demarrer-LeSourire.sh"
exit 0
