#!/bin/bash
# ============================================================================
# Sauvegarde de la base MariaDB sans passer par le client.
# Le dump (.sql) ET la cle de chiffrement sont ecrits dans le dossier
# "sauvegardes" du paquet : les deux vont toujours ensemble.
#
# Options : --dossier <chemin>   autre destination
#           --mysqldump <chemin>  chemin explicite (sinon recherche automatique)
# ============================================================================
set -uo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home

SERVEUR_HOME="${APP_HOME}/serveur"
JAR="${SERVEUR_HOME}/lesourire-serveur.jar"
export LESOURIRE_HOME="${APP_HOME}"
DOSSIER="${APP_HOME}/sauvegardes"

if [ ! -f "${JAR}" ]; then
    echo "JAR serveur introuvable : ${JAR}"
    exit 1
fi
if [ -f "${SERVEUR_HOME}/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${SERVEUR_HOME}/lesourire-serveur.conf.sh"
fi

echo "Sauvegarde de la base Le Sourire (lecture seule)"
echo "  Destination : ${DOSSIER}"
if [ -f "${APP_HOME}/fichiers/EMPREINTE.txt" ]; then
    cat "${APP_HOME}/fichiers/EMPREINTE.txt"
fi
echo

"${JAVA_HOME}/bin/java" -Dfile.encoding=UTF-8 \
    -Dloader.main=com.lesourire.serveur.outils.SauvegardeBase \
    -cp "${JAR}" org.springframework.boot.loader.launch.PropertiesLauncher \
    --dossier "${DOSSIER}" "$@"
CODE=$?

echo
if [ "${CODE}" -eq 0 ]; then
    echo "Sauvegarde terminee. Le fichier .sql et la cle sont dans :"
    echo "  ${DOSSIER}"
else
    echo "ECHEC de la sauvegarde (code ${CODE}) : lisez le message ci-dessus."
fi
exit "${CODE}"