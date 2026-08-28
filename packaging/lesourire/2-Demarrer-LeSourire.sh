#!/bin/bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
CLIENT_HOME="${APP_HOME}/client"
# shellcheck disable=SC1091
source "${APP_HOME}/runtime-unix.sh"
lesourire_java_home
lesourire_jfx_dir

if [ ! -x "${JAVA_HOME}/bin/java" ]; then
    echo "Java Runtime introuvable (jre-${LESOURIRE_OS}/)."
    echo "Copiez une JRE 21 dans ce dossier (voir JRE-A-COPIER.txt)."
    exit 1
fi

if [ ! -f "${CLIENT_HOME}/lesourire-client.jar" ]; then
    echo "Client introuvable : ${CLIENT_HOME}/lesourire-client.jar"
    exit 1
fi

if [ ! -d "${JFX_DIR}" ]; then
    echo "Dossier JavaFX manquant : ${JFX_DIR}"
    exit 1
fi

MODULE_PATH="${CLIENT_HOME}:${CLIENT_HOME}/lib:${JFX_DIR}"

exec "${JAVA_HOME}/bin/java" \
    -Dfile.encoding=UTF-8 \
    --module-path "${MODULE_PATH}" \
    --module com.lesourire.client/com.lesourire.client.LeSourireClient
