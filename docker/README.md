# SIGRC - Despliegue Docker

## Requisitos

- Docker Engine 24+
- Docker Compose v2+
- Git

## Instalación

1. Clonar el repositorio:
```bash
git clone https://github.com/epmapa/sigrc.git
cd sigrc
```

2. Configurar variables de entorno:
```bash
cp .env.example .env
# Editar .env con los valores correctos
```

3. Iniciar los servicios:
```bash
cd docker
docker compose up -d --build
```

4. Verificar que los servicios estén funcionando:
```bash
docker compose ps
```

## Acceso

- **Frontend:** http://localhost
- **Backend API:** http://localhost/api/v1
- **Swagger UI:** http://localhost/api/v1/swagger-ui.html
- **Base de Datos:** localhost:5432

## Usuarios por Defecto

*Se deben crear mediante el script de datos iniciales o API.*

## Comandos Útiles

```bash
# Ver logs
docker compose logs -f

# Detener servicios
docker compose down

# Reconstruir un servicio específico
docker compose up -d --build backend

# Backup de base de datos
docker exec sigrc-db pg_dump -U sigrc_user sigrc > backup.sql

# Restaurar base de datos
cat backup.sql | docker exec -i sigrc-db psql -U sigrc_user sigrc
```

## Estructura de Volúmenes

- `postgres_data`: Datos persistentes de PostgreSQL
- `uploads_data`: Archivos adjuntos subidos por los usuarios
