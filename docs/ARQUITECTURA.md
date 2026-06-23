# SIGRC - Arquitectura del Sistema

## Visión General

SIGRC (Sistema Institucional de Gestión de Requerimientos, Cambios y Auditoría Tecnológica) es una plataforma integral diseñada para EPMAPA-T que permite gestionar tickets, cambios, versiones y auditoría con un enfoque en trazabilidad, control interno y respaldo documental para auditorías gubernamentales.

## Arquitectura de Capas

```
┌─────────────────────────────────────────────────────┐
│                   Frontend Angular                   │
│              PrimeNG + Tailwind CSS                   │
├─────────────────────────────────────────────────────┤
│                   API REST (HTTP/HTTPS)               │
├─────────────────────────────────────────────────────┤
│              Backend Spring Boot 3 + Java 21          │
│   ┌─────────┐ ┌──────────┐ ┌──────────────────────┐ │
│   │  Web    │ │ Services │ │  Infrastructure       │ │
│   │Controllers│ │ Business  │ │  Audit, Security     │ │
│   └─────────┘ └──────────┘ └──────────────────────┘ │
├─────────────────────────────────────────────────────┤
│              JPA / Hibernate ORM                      │
├─────────────────────────────────────────────────────┤
│              PostgreSQL 16                            │
└─────────────────────────────────────────────────────┘
```

## Principios de Diseño

1. **Trazabilidad Total**: Cada acción queda registrada en la tabla de auditoría con usuario, fecha, IP, datos anteriores y nuevos.
2. **Integridad Referencial**: Todas las relaciones FK están implementadas a nivel de base de datos.
3. **Seguridad por Capas**: JWT + roles + permisos a nivel de endpoint.
4. **Escalabilidad Horizontal**: Backend stateless, frontend estático servido por Nginx.
5. **Despliegue Dockerizado**: Contenedores independientes para cada componente.

## Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|-----------|---------|
| Backend | Spring Boot | 3.4 |
| Lenguaje | Java | 21 |
| Frontend | Angular | 20 |
| UI | PrimeNG | 19 |
| CSS | Tailwind CSS | 3.4 |
| Base de Datos | PostgreSQL | 16 |
| Contenedores | Docker + Docker Compose | latest |
| Proxy | Nginx | Alpine |
| API Doc | SpringDoc OpenAPI | 2.6 |

## Estructura del Proyecto

```
SIGRC/
├── sigrc-backend/          # Backend Spring Boot
│   ├── src/main/java/      # Código fuente Java
│   └── src/main/resources/ # Configuración
├── sigrc-frontend/         # Frontend Angular
│   └── src/app/            # Módulos de la aplicación
├── database/               # Scripts SQL
│   └── 01_ddl.sql          # DDL completo
├── docker/                 # Configuración Docker
│   ├── docker-compose.yml
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── nginx.conf
└── docs/                   # Documentación
```

## Módulos del Sistema

1. **Dashboard**: Indicadores en tiempo real, gráficos estadísticos, cumplimiento SLA.
2. **Tickets**: CRUD completo con filtros, historial de estados, comentarios y adjuntos.
3. **Cambios**: Solicitud, aprobación, implementación y verificación de cambios.
4. **Versiones**: Control de versiones por sistema con tickets asociados.
5. **Auditoría**: Registro inmutable de todas las acciones del sistema.
6. **Usuarios**: Gestión de usuarios, roles y permisos configurables.
7. **Base de Conocimiento**: Artículos, FAQs, manuales y procedimientos.
8. **Reportes**: Exportación a PDF y Excel con indicadores de gestión.

## Seguridad

- Autenticación mediante JWT (access + refresh token)
- Passwords hasheados con BCrypt
- Roles: ADMIN, JEFE_TI, TECNICO, AUDITOR, SUPERVISOR, SOLICITANTE
- Permisos configurables por rol
- Bloqueo automático tras 5 intentos fallidos
- Auditoría obligatoria de toda operación CRUD
