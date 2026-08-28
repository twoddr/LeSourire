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
