#!/bin/bash
# Assemble deux paquets distincts :
#   out/executables/lesourire-windows/      (.bat, JavaFX win, JRE Windows)
#   out/executables/lesourire-mac-linux/    (.sh, JavaFX linux/mac, JRE Linux/mac)
#
# Règle d'or : les artefacts (commun / client / serveur) viennent UNIQUEMENT
# des dossiers target/ du reactor courant — jamais d'un vieux ~/.m2.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_WIN="${ROOT}/out/executables/lesourire-windows"
OUT_UNIX="${ROOT}/out/executables/lesourire-mac-linux"
ANCIEN="${ROOT}/out/executables/lesourire"
PACKAGING="${ROOT}/packaging/lesourire"
VERSION="$(sed -n 's/.*<revision>\([^<]*\)<\/revision>.*/\1/p' "${ROOT}/pom.xml" | head -1)"
if [ -z "${VERSION}" ]; then
    VERSION="$(sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' "${ROOT}/pom.xml" | head -1)"
fi
if [ -z "${VERSION}" ] || [[ "${VERSION}" == *'${'* ]]; then
    echo "Impossible de lire la version (<revision>) dans pom.xml"
    exit 1
fi
JFX_VERSION="$(sed -n 's/.*<javafx.version>\([^<]*\)<\/javafx.version>.*/\1/p' "${ROOT}/pom.xml" | head -1)"

COMMUN_JAR="${ROOT}/commun/target/lesourire-commun-${VERSION}.jar"
CLIENT_JAR="${ROOT}/client/target/lesourire-client-${VERSION}.jar"
SERVEUR_JAR="${ROOT}/serveur/target/lesourire-serveur-${VERSION}.jar"
LIB_STAGE="${ROOT}/out/executables/.lib-client"

if [ -z "${JFX_VERSION}" ]; then
    echo "Impossible de lire javafx.version dans pom.xml"
    exit 1
fi

echo "==> Compilation Maven propre (clean install)"
(cd "${ROOT}" && mvn -q clean install -DskipTests)

for f in "${COMMUN_JAR}" "${CLIENT_JAR}" "${SERVEUR_JAR}"; do
    if [ ! -f "$f" ]; then
        echo "ERREUR: artefact manquant après build : $f"
        exit 1
    fi
done

echo "==> Dépendances tierces client (hors JavaFX, hors lesourire-commun)"
rm -rf "${LIB_STAGE}"
mkdir -p "${LIB_STAGE}"
(cd "${ROOT}" && mvn -q -pl client dependency:copy-dependencies \
    -DoutputDirectory="${LIB_STAGE}" \
    -DincludeScope=runtime \
    -DexcludeGroupIds=org.openjfx \
    -DexcludeArtifactIds=lesourire-commun)
cp -f "${COMMUN_JAR}" "${LIB_STAGE}/lesourire-commun-${VERSION}.jar"

copier_jfx() {
    local os="$1"
    local dest="$2"
    mkdir -p "${dest}"
    rm -f "${dest}/"*.jar
    for art in javafx-base javafx-graphics javafx-controls; do
        (cd "${ROOT}" && mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.7.1:copy \
            -Dartifact="org.openjfx:${art}:${JFX_VERSION}:jar:${os}" \
            -DoutputDirectory="${dest}")
    done
}

forcer_crlf() {
    local f="$1"
    [ -f "$f" ] || return 0
    python3 - "$f" <<'PY'
import sys
from pathlib import Path
p = Path(sys.argv[1])
data = p.read_bytes().replace(b"\r\n", b"\n").replace(b"\r", b"\n").replace(b"\n", b"\r\n")
p.write_bytes(data)
PY
}

reutiliser_jre() {
    local src="$1"
    local dest="$2"
    if [ -e "${dest}/bin" ] || [ -e "${dest}/Contents" ]; then
        return 0
    fi
    if [ -e "${src}/bin" ] || [ -e "${src}/Contents" ]; then
        echo "    réutilisation JRE : ${src} → ${dest}"
        mkdir -p "$(dirname "${dest}")"
        rm -rf "${dest}"
        cp -a "${src}" "${dest}"
    else
        mkdir -p "${dest}"
    fi
}

verifier_classe() {
    local jar="$1"
    local classe="$2"
    if ! jar tf "${jar}" | grep -q "${classe}"; then
        echo "ERREUR: ${jar} ne contient pas ${classe}"
        exit 1
    fi
}

copier_artefacts() {
    local dest="$1"
    mkdir -p "${dest}/serveur" "${dest}/client/lib" "${dest}/sql" "${dest}/sauvegardes"
    cp -f "${SERVEUR_JAR}" "${dest}/serveur/lesourire-serveur.jar"
    cp -f "${CLIENT_JAR}" "${dest}/client/lesourire-client.jar"
    rm -f "${dest}/client/lib/"*.jar
    cp -f "${LIB_STAGE}/"*.jar "${dest}/client/lib/"
    cp -f "${PACKAGING}/sql/01_creer_bd.sql" "${dest}/sql/"
    : > "${dest}/sauvegardes/.gitkeep"

    cmp -s "${COMMUN_JAR}" "${dest}/client/lib/lesourire-commun-${VERSION}.jar" \
        || { echo "ERREUR: lesourire-commun assemblé ≠ commun/target"; exit 1; }
    cmp -s "${CLIENT_JAR}" "${dest}/client/lesourire-client.jar" \
        || { echo "ERREUR: lesourire-client assemblé ≠ client/target"; exit 1; }
    cmp -s "${SERVEUR_JAR}" "${dest}/serveur/lesourire-serveur.jar" \
        || { echo "ERREUR: lesourire-serveur assemblé ≠ serveur/target"; exit 1; }

    verifier_classe "${dest}/client/lib/lesourire-commun-${VERSION}.jar" 'FactureDTO'
    verifier_classe "${dest}/client/lesourire-client.jar" 'TableauBordVue'
    verifier_classe "${dest}/serveur/lesourire-serveur.jar" 'LeSourireServeurApplication'

    {
        echo "Le Sourire build $(date -Iseconds)"
        echo "version=${VERSION}"
        echo "commit=$(git -C "${ROOT}" rev-parse --short HEAD 2>/dev/null || echo '?')"
        echo
        sha256sum \
            "${dest}/serveur/lesourire-serveur.jar" \
            "${dest}/client/lesourire-client.jar" \
            "${dest}/client/lib/lesourire-commun-${VERSION}.jar"
    } > "${dest}/BUILD-INFO.txt"
}

echo "==> Paquet Windows : ${OUT_WIN}"
mkdir -p "${OUT_WIN}"
copier_artefacts "${OUT_WIN}"
cp -f "${PACKAGING}/LIREMOI-windows.txt" "${OUT_WIN}/README.txt"
cp -f "${PACKAGING}/JRE-A-COPIER-windows.txt" "${OUT_WIN}/JRE-A-COPIER.txt"
cp -f "${PACKAGING}/1-Demarrer-Serveur.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire-debug.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/ToutDemarrer.bat" "${OUT_WIN}/"
rm -f "${OUT_WIN}"/*.sh "${OUT_WIN}/serveur/"*.sh
rm -rf "${OUT_WIN}/client/javafx-linux" "${OUT_WIN}/client/javafx-mac" \
    "${OUT_WIN}/client/javafx-mac-aarch64" "${OUT_WIN}/jre-linux" "${OUT_WIN}/jre-mac"
for bat in "${OUT_WIN}"/*.bat "${OUT_WIN}/serveur"/*.bat; do
    forcer_crlf "${bat}"
done
if [ ! -f "${OUT_WIN}/serveur/lesourire-serveur.conf.bat" ]; then
    cp "${PACKAGING}/serveur/lesourire-serveur.conf.bat" "${OUT_WIN}/serveur/"
    forcer_crlf "${OUT_WIN}/serveur/lesourire-serveur.conf.bat"
fi
copier_jfx win "${OUT_WIN}/client/javafx-windows"
reutiliser_jre "${ANCIEN}/jre-windows" "${OUT_WIN}/jre-windows"
reutiliser_jre "${ROOT}/dist/LeSourire/java" "${OUT_WIN}/jre-windows"
if [ ! -f "${OUT_WIN}/jre-windows/bin/javaw.exe" ] && [ ! -f "${OUT_WIN}/jre-windows/bin/java.exe" ]; then
    echo "NOTE: jre-windows/ est vide — copiez une JRE 21 (voir JRE-A-COPIER.txt)"
fi
cat "${OUT_WIN}/BUILD-INFO.txt"

echo "==> Paquet Mac/Linux : ${OUT_UNIX}"
mkdir -p "${OUT_UNIX}"
copier_artefacts "${OUT_UNIX}"
cp -f "${PACKAGING}/LIREMOI-unix.txt" "${OUT_UNIX}/README.txt"
cp -f "${PACKAGING}/JRE-A-COPIER-unix.txt" "${OUT_UNIX}/JRE-A-COPIER.txt"
cp -f "${PACKAGING}/runtime-unix.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/1-Demarrer-Serveur.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire-debug.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/ToutDemarrer.sh" "${OUT_UNIX}/"
chmod +x "${OUT_UNIX}/runtime-unix.sh" \
    "${OUT_UNIX}/1-Demarrer-Serveur.sh" \
    "${OUT_UNIX}/2-Demarrer-LeSourire.sh" \
    "${OUT_UNIX}/2-Demarrer-LeSourire-debug.sh" \
    "${OUT_UNIX}/ToutDemarrer.sh"
rm -f "${OUT_UNIX}"/*.bat "${OUT_UNIX}/serveur/"*.bat
rm -rf "${OUT_UNIX}/client/javafx-windows" "${OUT_UNIX}/jre-windows"
if [ ! -f "${OUT_UNIX}/serveur/lesourire-serveur.conf.sh" ]; then
    cp "${PACKAGING}/serveur/lesourire-serveur.conf.sh" "${OUT_UNIX}/serveur/"
fi
echo "==> JavaFX Linux + macOS (Intel et Apple Silicon)"
copier_jfx linux "${OUT_UNIX}/client/javafx-linux"
copier_jfx mac "${OUT_UNIX}/client/javafx-mac"
copier_jfx mac-aarch64 "${OUT_UNIX}/client/javafx-mac-aarch64"
reutiliser_jre "${ANCIEN}/jre-linux" "${OUT_UNIX}/jre-linux"
reutiliser_jre "${ANCIEN}/jre-mac" "${OUT_UNIX}/jre-mac"
if [ ! -x "${OUT_UNIX}/jre-linux/bin/java" ]; then
    echo "NOTE: jre-linux/ est vide — copiez une JRE 21 (voir JRE-A-COPIER.txt)"
fi
if [ ! -x "${OUT_UNIX}/jre-mac/bin/java" ] && [ ! -x "${OUT_UNIX}/jre-mac/Contents/Home/bin/java" ]; then
    echo "NOTE: jre-mac/ est vide — copiez une JRE 21 (voir JRE-A-COPIER.txt)"
fi
cat "${OUT_UNIX}/BUILD-INFO.txt"

rm -rf "${LIB_STAGE}"

echo
echo "OK — deux dossiers prêts (artefacts = target du build courant) :"
echo "  ${OUT_WIN}"
echo "  ${OUT_UNIX}"
echo
if [ -d "${ANCIEN}" ]; then
    echo "Note : l'ancien dossier mixte n'est plus produit :"
    echo "  ${ANCIEN}"
    echo "  Vous pouvez le supprimer après vérification (les JRE utiles ont été recopiées)."
fi
echo
echo "Prochaines étapes :"
echo "  1. Vérifier/copier les JRE 21 (jre-windows, jre-linux, jre-mac)"
echo "  2. Adapter serveur/lesourire-serveur.conf.* dans chaque paquet"
echo "  3. Première install BD : sql/01_creer_bd.sql"
echo "  4. Zipper le dossier correspondant à l'OS du client"
