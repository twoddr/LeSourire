#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
CLIENT_HOME="${APP_HOME}/client"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home
lesourire_jfx_dir

echo "OS détecté : ${LESOURIRE_OS} / JavaFX : ${LESOURIRE_JFX_CLASSIFIER}"
echo "JAVA_HOME  : ${JAVA_HOME}"
echo

if [ ! -x "${JAVA_HOME}/bin/java" ]; then
    echo "Java Runtime introuvable (jre-${LESOURIRE_OS}/)."
    exit 1
fi

MODULE_PATH="${CLIENT_HOME}:${CLIENT_HOME}/lib:${JFX_DIR}"

echo "Lancement du client Le Sourire (mode debug)..."
echo

"${JAVA_HOME}/bin/java" \
    -Dfile.encoding=UTF-8 \
    --module-path "${MODULE_PATH}" \
    --module com.lesourire.client/com.lesourire.client.LeSourireClient

echo
echo "Code sortie : $?"
