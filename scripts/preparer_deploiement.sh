#!/usr/bin/env bash
# ============================================================================
# Assemble le pack Windows « prêt à copier » dans dist/LeSourire/
#
# Usage :
#   ./scripts/preparer_deploiement.sh
#   ./scripts/preparer_deploiement.sh /chemin/vers/java-21-windows
#
# Si un chemin Java Windows est fourni, il est copié dans dist/LeSourire/java/
# Sinon un dossier java/ vide est préparé avec un fichier PLACEZ-JAVA-ICI.txt
# ============================================================================
set -euo pipefail

RACINE="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$RACINE/dist/LeSourire"
MODELE="$RACINE/deploy/modele"
JFX_VERSION="$(grep -oP '(?<=<javafx.version>)[^<]+' "$RACINE/pom.xml" | head -1)"
JAVA_WIN_SRC="${1:-}"

echo "==> Compilation Maven (+ install local pour résoudre commun)"
(cd "$RACINE" && mvn -q install -DskipTests)

echo "==> Structure $DIST"
rm -rf "$DIST"
mkdir -p "$DIST/serveur" "$DIST/client/modules" "$DIST/sql" "$DIST/sauvegardes" "$DIST/java"

# Modèle (scripts + notice)
cp -a "$MODELE/LISEZ-MOI.txt" "$DIST/"
cp -a "$MODELE/Demarrer-LeSourire.bat" "$DIST/"
cp -a "$MODELE/serveur/." "$DIST/serveur/"
cp -a "$MODELE/client/demarrer-client.bat" "$DIST/client/"
cp -a "$MODELE/sql/." "$DIST/sql/"
: > "$DIST/sauvegardes/.gitkeep"

# Serveur fat-jar
SERVEUR_JAR="$(ls "$RACINE"/serveur/target/lesourire-serveur-*.jar | grep -v '\.original$' | head -1)"
cp -a "$SERVEUR_JAR" "$DIST/serveur/lesourire-serveur.jar"
echo "    serveur : $(basename "$SERVEUR_JAR")"

# Client : toutes les dépendances runtime (depuis le reactor + repo local)
echo "==> Dépendances client → modules/"
(cd "$RACINE" && mvn -q -pl client dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="$DIST/client/modules")
# Jar client + commun (produits locaux, pas toujours dans copy-dependencies selon config)
cp -a "$RACINE"/client/target/lesourire-client-*.jar "$DIST/client/modules/"
cp -a "$RACINE"/commun/target/lesourire-commun-*.jar "$DIST/client/modules/"

# JavaFX : forcer les natives Windows (le build Linux tire sinon les jars linux)
echo "==> JavaFX $JFX_VERSION (classifier win)"
rm -f "$DIST"/client/modules/javafx-*-linux*.jar \
      "$DIST"/client/modules/javafx-*-mac*.jar \
      "$DIST"/client/modules/javafx-*-win*.jar \
      "$DIST"/client/modules/javafx-base-*.jar \
      "$DIST"/client/modules/javafx-graphics-*.jar \
      "$DIST"/client/modules/javafx-controls-*.jar

for art in javafx-base javafx-graphics javafx-controls; do
  (cd "$RACINE" && mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
    -Dartifact="org.openjfx:${art}:${JFX_VERSION}:jar:win" \
    -DoutputDirectory="$DIST/client/modules")
done

# Java embarqué (optionnel)
if [[ -n "$JAVA_WIN_SRC" ]]; then
  if [[ ! -x "$JAVA_WIN_SRC/bin/java" && ! -f "$JAVA_WIN_SRC/bin/java.exe" ]]; then
    echo "ERREUR : $JAVA_WIN_SRC ne contient pas bin/java(.exe)" >&2
    exit 1
  fi
  echo "==> Copie du runtime Java Windows depuis $JAVA_WIN_SRC"
  rsync -a --delete "$JAVA_WIN_SRC"/ "$DIST/java/"
else
  cat > "$DIST/java/PLACEZ-JAVA-ICI.txt" <<'EOF'
Placez ici votre dossier Java 21 pour Windows.

Résultat attendu :
  LeSourire\java\bin\java.exe

Exemple :
  copiez le contenu de votre « java-21 » dans ce dossier « java ».
EOF
fi

# Petit inventaire
NB_MODULES=$(find "$DIST/client/modules" -name '*.jar' | wc -l)
TAILLE=$(du -sh "$DIST" | cut -f1)

cat > "$DIST/VERSION.txt" <<EOF
Le Sourire — pack déploiement
Généré le : $(date -Iseconds)
Commit    : $(git -C "$RACINE" rev-parse --short HEAD 2>/dev/null || echo '?')
JavaFX    : ${JFX_VERSION} (win)
Modules   : ${NB_MODULES} jars
EOF

echo
echo "==> Pack prêt : $DIST  ($TAILLE, $NB_MODULES modules)"
echo "    1. Copiez votre java-21 Windows dans dist/LeSourire/java/  (si pas déjà fait)"
echo "    2. Zippez dist/LeSourire/ et envoyez-le sur le PC du cabinet"
echo "    3. Suivez LISEZ-MOI.txt"
