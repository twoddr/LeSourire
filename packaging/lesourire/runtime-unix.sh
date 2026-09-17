# Sourcé par les lanceurs Unix. Définit JAVA_HOME et JFX_DIR selon l'OS.
# Prérequis : APP_HOME (racine du paquet). CLIENT_HOME pour JavaFX.

lesourire_detecter_os() {
    case "$(uname -s)" in
        Darwin)
            LESOURIRE_OS=mac
            if [ "$(uname -m)" = "arm64" ]; then
                LESOURIRE_JFX_CLASSIFIER=mac-aarch64
            else
                LESOURIRE_JFX_CLASSIFIER=mac
            fi
            ;;
        *)
            LESOURIRE_OS=linux
            LESOURIRE_JFX_CLASSIFIER=linux
            ;;
    esac
}

lesourire_java_home() {
    lesourire_detecter_os
    local racine="${APP_HOME}/jre-${LESOURIRE_OS}"
    if [ -x "${racine}/Contents/Home/bin/java" ]; then
        JAVA_HOME="${racine}/Contents/Home"
    else
        JAVA_HOME="${racine}"
    fi
}

lesourire_jfx_dir() {
    lesourire_detecter_os
    JFX_DIR="${CLIENT_HOME}/javafx-${LESOURIRE_JFX_CLASSIFIER}"
}

# ---------------------------------------------------------------------------
# Indicateur visuel terminal : « spinner » animé + chronomètre affiché tant
# qu'une opération est en cours. Écrit sur stderr et seulement si stderr est un
# vrai terminal : aucune pollution en redirection ni dans les journaux.
# ---------------------------------------------------------------------------
LESOURIRE_INDICATEUR_PID=""

lesourire_indicateur_debut() {
    local message="$1"
    if [ ! -t 2 ]; then
        return 0
    fi
    if [ -n "${LESOURIRE_INDICATEUR_PID}" ]; then
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
    LESOURIRE_INDICATEUR_PID=$!
}

lesourire_indicateur_fin() {
    if [ -n "${LESOURIRE_INDICATEUR_PID}" ]; then
        kill "${LESOURIRE_INDICATEUR_PID}" 2>/dev/null || true
        wait "${LESOURIRE_INDICATEUR_PID}" 2>/dev/null || true
        LESOURIRE_INDICATEUR_PID=""
    fi
    if [ -t 2 ]; then
        printf '\r\033[K' >&2
    fi
    return 0
}

# ---------------------------------------------------------------------------
# Réouverture dans une console : un double-clic (par ex. Dolphin sous KDE
# Plasma) exécute le lanceur SANS terminal — l'indicateur et les journaux
# seraient alors invisibles. On rouvre donc une console (konsole, gnome-terminal
# ou xterm) pour les rendre visibles. Le marqueur LESOURIRE_DANS_TERMINAL
# empêche toute relance en boucle ; sans affichage (SSH/CI) on ne fait rien.
#   "$1" : chemin absolu du lanceur à relancer dans la console.
# ---------------------------------------------------------------------------
lesourire_reouvrir_terminal() {
    local script="$1"
    if [ -n "${LESOURIRE_DANS_TERMINAL:-}" ] || [ -t 1 ]; then
        return 0
    fi
    if [ -z "${WAYLAND_DISPLAY:-}" ] && [ -z "${DISPLAY:-}" ]; then
        return 0
    fi
    local suite='printf "\nAppuyez sur Entrée pour fermer…"; read -r _'
    local commande="export LESOURIRE_DANS_TERMINAL=1; \"${script}\"; ${suite}"
    if command -v konsole >/dev/null 2>&1; then
        exec konsole --workdir "$(dirname "${script}")" -e bash -c "${commande}"
    fi
    if command -v gnome-terminal >/dev/null 2>&1; then
        exec gnome-terminal -- bash -c "${commande}"
    fi
    if command -v xterm >/dev/null 2>&1; then
        exec xterm -e bash -c "${commande}"
    fi
    return 0
}

# ---------------------------------------------------------------------------
# Teste si le serveur a terminé son démarrage (le point de statut est public et
# n'existe qu'une fois le contexte Spring prêt, migrations Flyway comprises).
#   "$1" : port d'écoute.
# Retour : 0 = prêt, 1 = pas encore, 2 = aucun outil de test disponible.
# ---------------------------------------------------------------------------
lesourire_serveur_pret() {
    local port="$1"
    if command -v curl >/dev/null 2>&1; then
        curl -sf --connect-timeout 2 --max-time 3 \
            "http://127.0.0.1:${port}/api/systeme/statut" >/dev/null 2>&1
        return $?
    fi
    if command -v nc >/dev/null 2>&1; then
        nc -z 127.0.0.1 "${port}" >/dev/null 2>&1
        return $?
    fi
    return 2
}
