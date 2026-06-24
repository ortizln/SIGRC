#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$REPO_DIR/sigrc-frontend"

BASE_HREF=/sigrc/
API_URL=/sigrc/api
OUTPUT_DIR=/var/www/sigrc

GREEN='\033[0;32m'; NC='\033[0m'
info() { echo -e "${GREEN}[INFO]${NC} $1"; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root)      BASE_HREF=/; API_URL=/api/v1; shift ;;
    --output)    OUTPUT_DIR="$2"; shift 2 ;;
    --help)      echo "Uso: ./build-frontend.sh [--root] [--output DIR]"; exit 0 ;;
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

sudo mkdir -p "$OUTPUT_DIR"

info "Limpiando versión anterior..."
sudo rm -rf "${OUTPUT_DIR:?}"/*

sudo cp -r dist/browser/* "$OUTPUT_DIR/"
info "Frontend compilado y copiado a: $OUTPUT_DIR"
sudo nginx -t && sudo systemctl reload nginx
info "nginx recargado"
