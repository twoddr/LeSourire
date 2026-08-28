#!/usr/bin/env bash
# ============================================================================
# Ancien pack Windows unique (dist/LeSourire/).
# Préférez désormais : ./scripts/assembler-executables.sh
#   → out/executables/lesourire-windows/
#   → out/executables/lesourire-mac-linux/
#
# Ce script délègue à l'assembleur Labos-like (deux paquets).
# L'argument Java Windows optionnel n'est plus utilisé ici :
# copiez la JRE dans out/executables/lesourire-windows/jre-windows/
# ============================================================================
set -euo pipefail

RACINE="$(cd "$(dirname "$0")/.." && pwd)"

echo "NOTE: preparer_deploiement.sh délègue à assembler-executables.sh"
echo "      (paquets Windows + Mac/Linux, style Labos)."
echo

exec "${RACINE}/scripts/assembler-executables.sh"
