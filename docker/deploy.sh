#!/bin/bash
set -euo pipefail

# ==========================================================
# Script de despliegue SIGRC
# Uso: ./deploy.sh [opciones]
#
# Opciones:
#   --subdir        Despliega con base-href /sigrc/ (para servir bajo subdirectorio)
#   --env-file FILE  Ruta al archivo .env con variables de entorno
#   --restart-db    Reinicia el contenedor de PostgreSQL (borra datos existentes)
#   --help          Muestra esta ayuda
# ==========================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
cd "$REPO_DIR"

MODE="root"
ENV_FILE=""
RESTART_DB=false

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Parsear argumentos
while [[ $# -gt 0 ]]; do
  case "$1" in
    --subdir)    MODE="subdir"; shift ;;
    --env-file)  ENV_FILE="$2"; shift 2 ;;
    --restart-db) RESTART_DB=true; shift ;;
    --help)      sed -n '2,12p' "$0"; exit 0 ;;
    *)           error "Argumento desconocido: $1. Use --help para ver opciones." ;;
  esac
done

# Verificar prerequisitos
command -v docker >/dev/null 2>&1 || error "Docker no está instalado"
command -v docker-compose >/dev/null 2>&1 || command -v docker >/dev/null 2>&1 || error "Docker Compose no está instalado"

# Cargar .env si se especificó
if [[ -n "$ENV_FILE" ]]; then
  [[ -f "$ENV_FILE" ]] || error "Archivo .env no encontrado: $ENV_FILE"
  set -a; source "$ENV_FILE"; set +a
  info "Variables cargadas desde $ENV_FILE"
fi

# Configurar modo de despliegue
if [[ "$MODE" == "subdir" ]]; then
  export BASE_HREF=/sigrc/
  export API_URL=/sigrc/api
  export NGINX_CONF=nginx-subdir.conf
  export CORS_ORIGINS="${CORS_ORIGINS:-http://192.168.100.116}"
  info "Modo: subdirectorio (/sigrc/)"
else
  export BASE_HREF=/
  export API_URL=/api/v1
  export NGINX_CONF=nginx.conf
  export CORS_ORIGINS="${CORS_ORIGINS:-http://192.168.100.116}"
  info "Modo: raíz (/)"
fi

# Crear directorios necesarios
UPLOAD_DIR="${UPLOAD_PATH:-/data/sigrc/uploads}"
if [[ ! -d "$UPLOAD_DIR" ]]; then
  sudo mkdir -p "$UPLOAD_DIR"
  sudo chown 1000:1000 "$UPLOAD_DIR" 2>/dev/null || true
  info "Directorio de uploads creado: $UPLOAD_DIR"
fi

# Detener contenedores existentes
info "Deteniendo contenedores existentes..."
docker-compose -f docker/docker-compose.yml down --remove-orphans || true

# Reiniciar DB si se solicita
if [[ "$RESTART_DB" == true ]]; then
  warn "Eliminando volumen de PostgreSQL (se perderán todos los datos)..."
  docker volume rm sigrc_postgres_data 2>/dev/null || true
fi

# Construir imágenes
info "Construyendo imágenes Docker..."
docker-compose -f docker/docker-compose.yml build --no-cache

# Iniciar servicios
info "Iniciando servicios..."
docker-compose -f docker/docker-compose.yml up -d

# Esperar a que los servicios estén saludables
info "Esperando a que el backend esté listo..."
for i in $(seq 1 30); do
  if docker exec sigrc-backend wget -qO- http://localhost:8080/api/v1/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    info "Backend listo!"
    break
  fi
  if [[ "$i" -eq 30 ]]; then
    warn "El backend no respondió después de 30s. Verifique los logs con: docker logs sigrc-backend"
  fi
  sleep 2
done

# Mostrar estado final
echo ""
info "=== Despliegue completado ==="
echo ""
if [[ "$MODE" == "subdir" ]]; then
  echo "  Frontend: http://192.168.100.116/sigrc/"
  echo "  API:      http://192.168.100.116/sigrc/api/"
else
  echo "  Frontend: http://192.168.100.116/"
  echo "  API:      http://192.168.100.116/api/"
fi
echo ""
echo "  PostgreSQL:  localhost:5432"
echo "  Backend:     localhost:8080"
echo ""

# Mostrar logs recientes
docker-compose -f docker/docker-compose.yml logs --tail=20
