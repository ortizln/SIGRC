#!/bin/bash
set -euo pipefail

# ==========================================================
# Construye el frontend SIGRC para servir con nginx nativo
# Uso: ./build-frontend.sh [opciones]
#
# Opciones:
#   --subdir       Construye con base-href /sigrc/ (para subdirectorio)
#   --output DIR   Directorio de salida (default: ./dist)
#   --help         Muestra esta ayuda
# ==========================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$REPO_DIR/sigrc-frontend"

BASE_HREF=/
API_URL=/api/v1
OUTPUT_DIR="$REPO_DIR/dist"

GREEN='\033[0;32m'
NC='\033[0m'
info() { echo -e "${GREEN}[INFO]${NC} $1"; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --subdir)    BASE_HREF=/sigrc/; API_URL=/sigrc/api; shift ;;
    --output)    OUTPUT_DIR="$2"; shift 2 ;;
    --help)      sed -n '2,11p' "$0"; exit 0 ;;
    *)           echo "Argumento desconocido: $1"; exit 1 ;;
  esac
done

info "Instalando dependencias..."
cd "$FRONTEND_DIR"
npm ci

info "Inyectando API_URL=$API_URL en environment.prod.ts..."
sed -i "s|apiUrl:.*|apiUrl: '$API_URL',|" src/environments/environment.prod.ts

info "Compilando con base-href=$BASE_HREF..."
npm run build -- --configuration production --base-href "$BASE_HREF"

mkdir -p "$OUTPUT_DIR"
cp -r dist/browser/* "$OUTPUT_DIR/"
info "Frontend compilado en: $OUTPUT_DIR"
info "Copie estos archivos al directorio raíz de su nginx"
