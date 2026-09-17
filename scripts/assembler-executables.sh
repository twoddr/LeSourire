#!/bin/bash
# Assemble deux paquets distincts :
#   out/executables/lesourire-windows/      (.bat, JavaFX win, JRE Windows)
#   out/executables/lesourire-mac-linux/    (.sh, JavaFX linux/mac, JRE Linux/mac)
#
# Règle d'or : les artefacts (commun / client / serveur) viennent UNIQUEMENT
# des dossiers target/ du reactor courant — jamais d'un vieux ~/.m2.
set -euo pipefail

# ---------------------------------------------------------------------------
# Réouverture dans une console : un double-clic (par ex. Dolphin sous KDE
# Plasma) exécute le script SANS terminal — l'indicateur animé et les journaux
# seraient alors invisibles. On rouvre donc automatiquement konsole (ou xterm)
# pour les rendre visibles. Le marqueur LESOURIRE_DANS_TERMINAL empêche toute
# relance en boucle ; en SSH/CI (aucun affichage graphique) on ne tente rien.
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJET_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SCRIPT_ASSEMBLEUR="${SCRIPT_DIR}/assembler-executables.sh"

relancer_dans_terminal() {
    if [ -n "${LESOURIRE_DANS_TERMINAL:-}" ] || [ -t 1 ]; then
        return 0
    fi
    if [ -z "${WAYLAND_DISPLAY:-}" ] && [ -z "${DISPLAY:-}" ]; then
        return 0
    fi
    # La console relance le script puis attend une touche, afin de laisser
    # l'utilisateur lire l'indicateur et le message de fin.
    local suite='printf "\nAppuyez sur Entrée pour fermer…"; read -r _'
    if command -v konsole >/dev/null 2>&1; then
        exec konsole --workdir "${PROJET_DIR}" -e bash -c \
            "export LESOURIRE_DANS_TERMINAL=1; \"${SCRIPT_ASSEMBLEUR}\"; ${suite}"
    fi
    if command -v xterm >/dev/null 2>&1; then
        exec xterm -e bash -c \
            "export LESOURIRE_DANS_TERMINAL=1; \"${SCRIPT_ASSEMBLEUR}\"; ${suite}"
    fi
    return 0
}
relancer_dans_terminal

# ---------------------------------------------------------------------------
# Notifications bureau (facultatives). Sous KDE Plasma, kdialog est l'outil
# natif et souvent le seul présent (zenity et notify-send peuvent manquer).
# On détecte aussi les sessions Wayland (WAYLAND_DISPLAY), pas seulement X11.
# En SSH / sans affichage graphique, on se contente de la sortie terminal.
# ---------------------------------------------------------------------------
NOTIF_MODE=0
if [ -n "${WAYLAND_DISPLAY:-}" ] || [ -n "${DISPLAY:-}" ]; then
    if command -v kdialog >/dev/null 2>&1; then
        NOTIF_MODE=3
    elif command -v zenity >/dev/null 2>&1; then
        NOTIF_MODE=2
    elif command -v notify-send >/dev/null 2>&1; then
        NOTIF_MODE=1
    fi
fi
PROGRESS_PID=""

# ---------------------------------------------------------------------------
# Indicateur visuel terminal : « spinner » animé + chronomètre, affiché tant
# que le script travaille. Utile en SSH / sans affichage graphique, où zenity
# et notify-send ne peuvent rien montrer. Écrit sur stderr et seulement si
# stderr est un terminal : aucune pollution de stdout ni des redirections/CI.
# ---------------------------------------------------------------------------
SPINNER_PID=""

demarrer_indicateur() {
    local message="$1"
    if [ ! -t 2 ]; then
        return 0
    fi
    if [ -n "${SPINNER_PID}" ]; then
        return 0
    fi
    (
        local frames='|/-\' i=0 debut t
        debut=$(date +%s)
        while :; do
            t=$(( $(date +%s) - debut ))
            printf '\r\033[K  %s  %s  (%02d:%02d)' \
                "${frames:$((i % 4)):1}" "${message}" $((t / 60)) $((t % 60)) >&2
            i=$((i + 1))
            sleep 0.5
        done
    ) &
    SPINNER_PID=$!
}

arreter_indicateur() {
    if [ -n "${SPINNER_PID}" ]; then
        kill "${SPINNER_PID}" 2>/dev/null || true
        wait "${SPINNER_PID}" 2>/dev/null || true
        SPINNER_PID=""
    fi
    if [ -t 2 ]; then
        printf '\r\033[K' >&2
    fi
}

notifier_debut() {
    case "${NOTIF_MODE}" in
        3)
            # kdialog (KDE Plasma) : bulle passive, non bloquante. Le suivi
            # continu de la progression est assuré par l'indicateur du terminal.
            kdialog --title "Le Sourire" --passivepopup \
                "Création des exécutables en cours — suivez la progression dans la console." \
                20 >/dev/null 2>&1 || true
            ;;
        2)
            zenity --progress --pulsate --no-cancel \
                --title="Le Sourire" \
                --text="Création des exécutables en cours…" \
                >/dev/null 2>&1 &
            PROGRESS_PID=$!
            ;;
        1)
            notify-send -i system-run "Le Sourire" \
                "Création des exécutables en cours…" >/dev/null 2>&1 || true
            ;;
    esac
}

notifier_succes() {
    [ -n "${PROGRESS_PID}" ] && kill "${PROGRESS_PID}" 2>/dev/null || true
    PROGRESS_PID=""
    case "${NOTIF_MODE}" in
        3)
            kdialog --title "Le Sourire" --passivepopup \
                "Exécutables créés avec succès (lesourire-windows et lesourire-mac-linux)." \
                15 >/dev/null 2>&1 || true
            ;;
        2)
            zenity --info --title="Le Sourire" \
                --text="Exécutables créés avec succès.\n\nLes dossiers lesourire-windows et lesourire-mac-linux sont prêts." \
                >/dev/null 2>&1 || true
            ;;
        1)
            notify-send -i dialog-information "Le Sourire" \
                "Exécutables créés avec succès." >/dev/null 2>&1 || true
            ;;
    esac
}

notifier_echec() {
    [ -n "${PROGRESS_PID}" ] && kill "${PROGRESS_PID}" 2>/dev/null || true
    PROGRESS_PID=""
    case "${NOTIF_MODE}" in
        3)
            kdialog --title "Le Sourire" --passivepopup \
                "La création des exécutables a échoué — consultez la console pour le détail." \
                30 >/dev/null 2>&1 || true
            ;;
        2)
            zenity --error --title="Le Sourire" \
                --text="La création des exécutables a échoué.\nConsultez le terminal pour le détail." \
                >/dev/null 2>&1 || true
            ;;
        1)
            notify-send -u critical -i dialog-error "Le Sourire" \
                "La création des exécutables a échoué." >/dev/null 2>&1 || true
            ;;
    esac
}

sur_exit() {
    local code=$?
    arreter_indicateur
    [ -n "${PROGRESS_PID}" ] && kill "${PROGRESS_PID}" 2>/dev/null || true
    PROGRESS_PID=""
    if [ "${code}" -ne 0 ]; then
        if [ -t 2 ]; then
            printf '\r\033[K  \342\234\226  Terminé : la création des exécutables a échoué (code %d).\n' \
                "${code}" >&2
        fi
        notifier_echec
    else
        if [ -t 2 ]; then
            printf '\r\033[K  \342\234\224  Terminé : exécutables créés avec succès.\n' >&2
        fi
        notifier_succes
    fi
}
trap sur_exit EXIT

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
notifier_debut
demarrer_indicateur "Compilation Maven en cours…"
(cd "${ROOT}" && mvn -q clean install -DskipTests)
arreter_indicateur

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
cp -f "${PACKAGING}/LisezMoi-windows.txt" "${OUT_WIN}/README.txt"
cp -f "${PACKAGING}/JRE-A-COPIER-windows.txt" "${OUT_WIN}/JRE-A-COPIER.txt"
cp -f "${PACKAGING}/1-Demarrer-Serveur.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire-debug.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/ToutDemarrer.bat" "${OUT_WIN}/"
cp -f "${PACKAGING}/Arreter-Serveur.bat" "${OUT_WIN}/"
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
cp -f "${PACKAGING}/LisezMoi-unix.txt" "${OUT_UNIX}/README.txt"
cp -f "${PACKAGING}/JRE-A-COPIER-unix.txt" "${OUT_UNIX}/JRE-A-COPIER.txt"
cp -f "${PACKAGING}/runtime-unix.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/1-Demarrer-Serveur.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/2-Demarrer-LeSourire-debug.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/ToutDemarrer.sh" "${OUT_UNIX}/"
cp -f "${PACKAGING}/Arreter-Serveur.sh" "${OUT_UNIX}/"
chmod +x "${OUT_UNIX}/runtime-unix.sh" \
    "${OUT_UNIX}/1-Demarrer-Serveur.sh" \
    "${OUT_UNIX}/2-Demarrer-LeSourire.sh" \
    "${OUT_UNIX}/2-Demarrer-LeSourire-debug.sh" \
    "${OUT_UNIX}/ToutDemarrer.sh" \
    "${OUT_UNIX}/Arreter-Serveur.sh"
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
