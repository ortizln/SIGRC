#!/bin/bash
set -euo pipefail

# ==========================================================
# Script de despliegue SIGRC - Backend
# Uso: ./deploy.sh [opciones]
#
# Opciones:
#   --env-file FILE  Ruta al archivo .env con variables de entorno
#   --help           Muestra esta ayuda
# ==========================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
cd "$REPO_DIR"

ENV_FILE=""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)  ENV_FILE="$2"; shift 2 ;;
    --help)      sed -n '2,10p' "$0"; exit 0 ;;
    *)           error "Argumento desconocido: $1. Use --help para ver opciones." ;;
  esac
done

command -v docker >/dev/null 2>&1 || error "Docker no está instalado"
command -v docker-compose >/dev/null 2>&1 || command -v docker >/dev/null 2>&1 || error "Docker Compose no está instalado"

if [[ -n "$ENV_FILE" ]]; then
  [[ -f "$ENV_FILE" ]] || error "Archivo .env no encontrado: $ENV_FILE"
  set -a; source "$ENV_FILE"; set +a
  info "Variables cargadas desde $ENV_FILE"
fi

export CORS_ORIGINS="${CORS_ORIGINS:-http://192.168.100.116}"

UPLOAD_DIR="${UPLOAD_PATH:-/data/sigrc/uploads}"
if [[ ! -d "$UPLOAD_DIR" ]]; then
  sudo mkdir -p "$UPLOAD_DIR"
  sudo chown 1000:1000 "$UPLOAD_DIR" 2>/dev/null || true
  info "Directorio de uploads creado: $UPLOAD_DIR"
fi

info "Deteniendo contenedores existentes..."
docker-compose -f docker/docker-compose.yml down --remove-orphans || true

info "Construyendo imagen del backend..."
docker-compose -f docker/docker-compose.yml build --no-cache

info "Iniciando backend..."
docker-compose -f docker/docker-compose.yml up -d

info "Esperando a que el backend esté listo..."
for i in $(seq 1 30); do
  if docker exec sigrc-backend wget -qO- http://localhost:8080/api/v1/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    info "Backend listo!"
    break
  fi
  if [[ "$i" -eq 30 ]]; then
    warn "El backend no respondió después de 30s. Verifique con: docker logs sigrc-backend"
  fi
  sleep 2
done

echo ""
info "=== Despliegue completado ==="
echo ""
echo "  Backend API: http://192.168.100.116:8080/api/v1/"
echo "  DB Host:     ${DATABASE_HOST:-host.docker.internal}:${DATABASE_PORT:-5432}"
echo ""
echo "  Para construir el frontend ejecute:"
echo "    cd docker && ./build-frontend.sh [--subdir]"
echo ""

docker-compose -f docker/docker-compose.yml logs --tail=20
