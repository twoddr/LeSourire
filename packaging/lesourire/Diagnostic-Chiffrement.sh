#!/bin/bash
# ============================================================================
# Diagnostic du chiffrement Le Sourire (lecture seule par defaut).
# A lancer quand le client affiche des erreurs sur les listes, quand le serveur
# refuse de demarrer, ou apres une reinstanciation du dossier.
#
# Options utiles (transmises telles quelles) :
#   --afficher-cles                 affiche les cles trouvees en base64
#   --exporter-cle <fichier>        ecrit la cle qui relit les donnees
#   --verifier-cle <cle|fichier>    teste une cle (code 0 si tout est lisible)
#   --rechiffrer <cle|fichier> --je-confirme
#                                   reecrit la base sous une cle unique
#
# Le rapport est ecrit dans DIAGNOSTIC-CHIFFREMENT.txt, a cote de ce fichier.
# ============================================================================
set -uo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home

SERVEUR_HOME="${APP_HOME}/serveur"
JAR="${SERVEUR_HOME}/lesourire-serveur.jar"
RAPPORT="${APP_HOME}/DIAGNOSTIC-CHIFFREMENT.txt"
export LESOURIRE_HOME="${APP_HOME}"

if [ ! -f "${JAR}" ]; then
    echo "JAR serveur introuvable : ${JAR}"
    exit 1
fi

if [ -f "${SERVEUR_HOME}/lesourire-serveur.conf.sh" ]; then
    # shellcheck disable=SC1091
    source "${SERVEUR_HOME}/lesourire-serveur.conf.sh"
fi
export LESOURIRE_BD_URL="${LESOURIRE_BD_URL:-jdbc:mariadb://localhost:3306/lesourire}"
export LESOURIRE_BD_UTILISATEUR="${LESOURIRE_BD_UTILISATEUR:-admin}"
export LESOURIRE_BD_MOT_DE_PASSE="${LESOURIRE_BD_MOT_DE_PASSE:-csa-soft}"

echo "Diagnostic du chiffrement Le Sourire"
echo "  Base de données : ${LESOURIRE_BD_URL}"
echo "  Rapport         : ${RAPPORT}"
echo "  Sans option, cette analyse est en LECTURE SEULE (aucune donnée modifiée)."
echo

"${JAVA_HOME}/bin/java" -Dfile.encoding=UTF-8 \
    -Dloader.main=com.lesourire.serveur.outils.DiagnosticChiffrement \
    -cp "${JAR}" org.springframework.boot.loader.launch.PropertiesLauncher \
    --rapport "${RAPPORT}" "$@"
CODE=$?

echo
case "${CODE}" in
    0) echo "Résultat : toutes les données sont lisibles avec une clé connue (code 0)." ;;
    1) echo "Résultat : erreur d'exécution (code 1) — lisez le rapport." ;;
    2) echo "Résultat : des données ne sont relues par AUCUNE clé (code 2)." ;;
    3) echo "Résultat : réchiffrement partiel (code 3) — des lignes restent illisibles." ;;
esac
echo "Rapport complet : ${RAPPORT}"
exit "${CODE}"