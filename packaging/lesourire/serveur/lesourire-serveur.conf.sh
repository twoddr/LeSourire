# Configuration MariaDB / serveur Le Sourire (à adapter une fois chez le client)

export LESOURIRE_BD_URL="${LESOURIRE_BD_URL:-jdbc:mariadb://localhost:3306/lesourire}"
export LESOURIRE_BD_UTILISATEUR="${LESOURIRE_BD_UTILISATEUR:-admin}"
export LESOURIRE_BD_MOT_DE_PASSE="${LESOURIRE_BD_MOT_DE_PASSE:-csa-soft}"
export LESOURIRE_PORT="${LESOURIRE_PORT:-8420}"

# --- Notifications patients (SMS via l'API Orange Cameroun) -------------------
# Identifiants de l'application déclarée sur https://developer.orange.com
# (menu « Applications »). Ils ne sont JAMAIS stockés en base : la table
# `parametre` n'est pas chiffrée, ce fichier doit rester lisible par le seul
# compte du serveur.
# Laissés vides, le SMS reste en mode « journal » (simulé, rien n'est facturé).
export LESOURIRE_ORANGE_CLIENT_ID=""
export LESOURIRE_ORANGE_CLIENT_SECRET=""
# Adresse d'expédition : nom d'expéditeur whitelisté (« CABINET LE SOURIRE »),
# ou « tel:+237XXXXXXXXX » pour envoyer depuis une carte SIM Orange.
# Vide = paramètre `notification.nom_expediteur` (Administration > Paramètres).
export LESOURIRE_ORANGE_EXPEDITEUR=""

