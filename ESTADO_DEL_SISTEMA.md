# SIGRC — Estado Total del Sistema

> **Sistema Institucional de Gestión de Requerimientos, Cambios y Auditoría Tecnológica**
> EPMAPA-T — Documento generado: 2026-10-19

---

## 1. Datos Generales

| Aspecto | Detalle |
|---|---|
| **Nombre** | SIGRC v1.0.0 |
| **Institución** | EPMAPA-T |
| **Stack Backend** | Spring Boot 3.4.0 / Java 21 / PostgreSQL 16+ |
| **Stack Frontend** | Angular 17+ (standalone components) / Bootstrap 5 / PrimeIcons / SweetAlert2 |
| **Autenticación** | JWT stateless (BCrypt, filtros custom) |
| **Tiempo real** | WebSocket STOMP (`/topic/notificaciones`) |
| **Exportación** | Apache POI (Excel) + iText 7 (PDF) |
| **Arquitectura** | Monorepo: `sigrc-backend/` + `sigrc-frontend/` + migraciones SQL + docker/ |

---

## 2. Cifras Clave

| Métrica | Cantidad |
|---|---|
| **Endpoints API** | **194** en 18 controllers |
| **Entidades JPA** | 53 |
| **Repositorios** | 35 |
| **Servicios backend** | 25 |
| **Componentes frontend** | ~50 archivos TS en 26 módulos de features |
| **Rutas frontend** | 1 pública + 67 protegidas |
| **Migraciones SQL** | 13 (`migracion-*.sql`) + scripts de fix + reversa BCC |
| **Tablas en DDL base** | 22 (módulo TI original) |
| **Tests unitarios** | 48 (6 archivos, todos Mockito + AssertJ) |
| **Roles del sistema** | 6 (ADMIN, JEFE_TI, TECNICO, SOLICITANTE, AUDITOR, SUPERVISOR) |

---

## 3. Árbol de Módulos

```
SIGRC
├── 🔐 Seguridad & RBAC
│   ├── Autenticación JWT (login, perfil /me)
│   ├── Usuarios (CRUD, vincular empleado)
│   ├── Roles (CRUD, permisos por rol)
│   ├── Permisos (catálogo por módulo × tipo acceso)
│   └── Auditoría (log de cambios con snapshot JSON)
│
├── 📬 Correspondencia
│   ├── Registro (crear, editar, anular documentos)
│   ├── Flujo (recibir, sumillar, derivar, aprobar)
│   ├── Sentido (INGRESO/SALIDA con percepción por usuario)
│   ├── Bandejas (unidad, puesto, pendientes)
│   ├── Destinatarios (usuarios, áreas, institucional)
│   ├── Adjuntos (subir, descargar, eliminar)
│   ├── Historial completo de cambios
│   ├── Generación / vinculación de tickets
│   ├── Dashboard de documentos
│   └── Delegaciones aplicadas al flujo
│
├── 🎫 Tickets
│   ├── CRUD con filtros (estado, categoría, prioridad, SLA)
│   ├── Estados (nuevo → asignado → en proceso → resuelto → cerrado)
│   ├── Asignación responsable
│   ├── Comentarios (públicos / internos)
│   ├── Adjuntos (upload/download)
│   └── Historial de cambios
│
├── 🔄 Cambios
│   ├── CRUD (solicitudes de cambio)
│   ├── Flujo de aprobación (proponer → aprobar → implementar)
│   ├── Plan de archivo (carga/descarga)
│   └── Notificaciones WebSocket
│
├── 📦 Versiones
│   ├── CRUD de versiones de sistemas
│   ├── Vinculación a tickets
│   └── Control por sistema
│
├── 📊 Dashboards
│   ├── Dashboard general (tickets + documentos KPIs)
│   └── Dashboard Talento Humano (indicadores RRHH)
│
├── 📚 Base de Conocimiento
│   └── Gestión de artículos de conocimiento
│
├── 👥 Talento Humano (RRHH)
│   ├── 🏗 Estructura Organizacional
│   │   ├── Niveles jerárquicos (CRUD)
│   │   ├── Unidades organizacionales (CRUD + mover drag&drop)
│   │   ├── Organigrama visual (árbol jerárquico)
│   │   └── Responsable de unidad (asignación)
│   │
│   ├── 💼 Puestos de Trabajo
│   │   ├── CRUD de puestos
│   │   ├── Perfil completo (funciones, formación, experiencia,
│   │   │   capacitación, productos, interfaces)
│   │   └── Requisitos vs. empleado (matriz)
│   │
│   ├── 👤 Empleados
│   │   ├── CRUD + expediente completo
│   │   ├── Formación académica (con verificación Senescyt)
│   │   ├── Experiencia laboral
│   │   ├── Capacitaciones
│   │   ├── Documentos con confidencialidad (§30)
│   │   │   └── Niveles: Público, Interno, Confidencial RRHH, Restringido
│   │   ├── Repositorio físico de archivos (§29)
│   │   │   └── UUID + hash SHA-256 + mime + tamaño
│   │   ├── Mi expediente (auto-consulta con ocultamiento)
│   │   └── Creación con opción de crear usuario vinculado ✅ NUEVO
│   │
│   ├── 📋 Asignaciones de Puesto
│   │   ├── Asignación (cierra anterior automáticamente)
│   │   ├── Finalización
│   │   ├── Historial por empleado
│   │   ├── Asignación actual / todas activas
│   │   └── Jefatura por empleado
│   │
│   ├── 🔄 Movimientos de Personal
│   │   ├── Creación (traslado, ascenso, reubicación)
│   │   ├── Workflow: borrador → enviado → aprobado/rechazado
│   │   └── Ejecución (genera nueva asignación)
│   │
│   ├── 📄 Acciones de Personal
│   │   ├── CRUD (documentos legales)
│   │   ├── Workflow: borrador → revisión → aprobado/rechazado
│   │   └── Estados con trazabilidad
│   │
│   ├── 🏖 Vacaciones y Permisos
│   │   ├── Solicitud (vacación, permiso, licencia)
│   │   ├── Aprobación por jefe inmediato (validación estructural)
│   │   ├── Aprobación final por Talento Humano
│   │   ├── Rechazo / anulación
│   │   └── Auto-creación de delegación al aprobar ✅ INTEGRADO
│   │
│   ├── 📑 Distributivo
│   │   ├── Consulta con filtros
│   │   └── Exportación Excel / PDF
│   │
│   ├── 📖 Manual de Funciones
│   │   ├── Lectura en vivo desde estructura + puestos
│   │   ├── Crea direcciones / unidades / puestos inline (admin)
│   │   ├── Drag & Drop de unidades y puestos
│   │   ├── Versionado: borrador → vigente → derogado
│   │   └── Propagación automática de cambios
│   │
│   └── 🤝 Delegaciones de Función
│       ├── Creación (origen → delegada, con fechas)
│       ├── Cancelación / finalización
│       ├── Resolución de cadenas de delegación
│       ├── Integración con correspondencia (sumilla, derivación)
│       └── Integración con vacaciones (auto-crear) ✅
│
├── ⚙️ Administración
│   ├── Catálogos (áreas, sistemas, categorías, subcategorías)
│   ├── Reportes (exportación)
│   ├── App Móvil (gestión APK)
│   └── Carrusel (contenido destacado)
│
└── 🔒 Confidencialidad (§30)
    ├── Control de acceso por nivel en documentos de empleado
    ├── Auto-consulta oculta confidenciales
    └── Auditoría de acceso a documentos sensibles
```

---

## 4. Endpoints por Módulo (194 total)

| Controller | Base Path | Endpoints | Módulo |
|---|---|---|---|
| TalentoHumanoController | `/talento-humano` | **36** | RRHH |
| CorrespondenciaController | `/correspondencia` | **28** | Correspondencia |
| CatalogoController | `/catalogos` | **21** | Catálogos |
| GestionPersonalController | `/talento-humano` | **21** | RRHH (movimientos, acciones, ausencias) |
| BandejaCompartidaController | `/correspondencia` | **12** | BCC (bandeja compartida controlada) |
| UsuarioController | `/usuarios` | **10** | Seguridad |
| TicketController | `/tickets` | **9** | Tickets |
| CambioController | `/cambios` | **8** | Cambios |
| DashboardController | `/dashboard` | **8** | Dashboard |
| AppMovilController | `/app-movil` | **7** | App Móvil |
| VersionController | `/versiones` | **6** | Versiones |
| ManualFuncionesController | `/talento-humano/manual-funciones` | **6** | RRHH |
| RolController | `/roles` | **5** | Seguridad |
| PermisoController | `/permisos` | **5** | Seguridad |
| TicketAdjuntoController | `/tickets/{id}/adjuntos` | **5** | Tickets |
| DelegacionController | `/talento-humano/delegaciones` | **4** | RRHH |
| AuthController | `/auth` | **2** | Seguridad |
| AuditoriaController | `/auditoria` | **1** | Auditoría |
| **TOTAL** | | **194** | |

### 4.1 Endpoints BCC — Bandeja Compartida Controlada

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/correspondencia/vinculos-asistente` | ADMIN | Listar vínculos asistente→titular |
| POST | `/correspondencia/vinculos-asistente` | ADMIN | Crear vínculo |
| PATCH | `/correspondencia/vinculos-asistente/{id}` | ADMIN | Editar vínculo |
| POST | `/correspondencia/vinculos-asistente/{id}/desactivar` | ADMIN | Revocar vínculo |
| GET | `/correspondencia/mis-bandejas-compartidas` | Autenticado | Titulares vinculados al asistente |
| GET | `/correspondencia/bandeja-compartida/{puestoId}` | Vínculo vigente | Documentos del titular |
| POST | `/correspondencia/recepcion-delegada` | Vínculo + `puedeRecepcionar` | Recepción con identidad del asistente |
| PUT | `/correspondencia/{id}/lectura-personal` | Autenticado | Lectura propia (no global) |
| GET | `/correspondencia/lecturas-personales?ids=` | Autenticado | IDs leídos por el usuario |
| GET | `/correspondencia/verificar-vinculo/{puestoTitularId}` | Autenticado | Estado del vínculo |
| GET | `/correspondencia/{id}/restriccion-asistente` | Autenticado | ¿Vedado para sumillar/derivar? |
| GET | `/correspondencia/{id}/seguimiento` | Participante o asistente con `puedeSeguimiento` | Trazabilidad autorizada |

---

## 5. Modelo de Datos

### 5.1 Tablas DDL base (22) — Módulo TI original

```
roles, permisos, roles_permisos, areas, sistemas, usuarios, categorías,
subcategorias, slas, tickets, ticket_historial, ticket_comentarios,
ticket_adjuntos, cambios, versiones, versiones_tickets, base_conocimiento,
auditoria, notificaciones, indicadores_cache, configuracion, sesiones
```

### 5.2 Tablas creadas por migraciones (módulos Correspondencia + TH)

```
── Correspondencia ──
correspondencia, correspondencia_adjunto, correspondencia_area,
correspondencia_destinatario, correspondencia_documento_tipo,
correspondencia_historial, correspondencia_responsable (con sumilla),
correspondencia_respuesta, correspondencia_ticket, correspondencia_referencia,
correspondencia_vinculo_asistente, correspondencia_lectura_usuario,
correspondencia_recepcion_idempotente (BCC)

── Talento Humano ──
nivel_organizacional, unidad_organizacional, puesto (+perfil: funciones,
formación, experiencia, capacitación, productos, interfaces),
empleado, empleado_documento, empleado_formacion, empleado_experiencia,
empleado_capacitacion, asignacion_puesto,
movimiento_personal, accion_personal, solicitud_ausencia,
delegacion_funcion, version_manual
```

### 5.3 Entidades JPA (53)

| Categoría | Cantidad | Entidades |
|---|---|---|
| Seguridad/RBAC | 6 | Usuario, Rol, Permiso, RolesPermiso, UsuarioPermiso, Notificacion |
| Catálogos TI | 4 | Area, Sistema, Categoria, Subcategoria |
| Tickets | 5 | Ticket, TicketHistorial, TicketComentario, TicketAdjunto, Sla |
| Cambios/Versiones | 4 | Cambio, Version, VersionTicket, VersionManual |
| Conocimiento/Auditoría/Móvil | 3 | BaseConocimiento, Auditoria, AppMovil |
| **Correspondencia** | **12** | Correspondencia, Adjunto, Area, Destinatario, DocumentoTipo, Historial, Responsable, Respuesta, Ticket, **VinculoAsistente, LecturaUsuario, RecepcionIdempotente (BCC)** |
| **TH — Estructura** | 3 | NivelOrganizacional, UnidadOrganizacional, AsignacionPuesto |
| **TH — Puestos** | 7 | Puesto, Funcion, Formacion, Experiencia, Capacitacion, Producto, Interfaz |
| **TH — Empleados** | 5 | Empleado, Documento, Formacion, Experiencia, Capacitacion |
| **TH — Flujos** | 4 | MovimientoPersonal, AccionPersonal, SolicitudAusencia, DelegacionFuncion |

---

## 6. Seguridad y Control de Acceso

### 6.1 Roles

| Rol | Descripción | Alcance principal |
|---|---|---|
| `ADMIN` | Administrador total | Todo el sistema |
| `JEFE_TI` | Jefe de TI | Tickets, cambios, versiones, correspondencia |
| `TECNICO` | Técnico de soporte | Tickets (asignados), creación |
| `SOLICITANTE` | Usuario final | Crear tickets, ver propios |
| `AUDITOR` | Auditor | Lectura, auditoría |
| `SUPERVISOR` | Supervisor | Dashboard, correspondencia |

### 6.2 Mecanismos

- **JWT stateless** con filtro `JwtAuthenticationFilter` → `UserPrincipal(username, idUsuario, rol)`
- **BCrypt** para passwords, `debe_cambiar_password` en primer login
- **@PreAuthorize** a nivel de método y clase en todos los controllers
- **Frontend guards**: `authGuard` (67 rutas), `roleGuard(ADMIN)` (16 rutas), `roleGuard(ADMIN, AUDITOR)` (1 ruta)
- **Confidencialidad §30**: documentos de empleado con 4 niveles de acceso; auto-consulta oculta sensibles
- **Auditoría §31**: todos los cambios registrados con snapshot JSON antes/después

### 6.3 Módulos con permiso granular

| Módulo | Tipos de acceso |
|---|---|
| DASHBOARD, CORRESPONDENCIA, TICKETS, CAMBIOS, VERSIONES | LECTURA, ESCRITURA |
| TALENTO_HUMANO, AUDITORIA, USUARIOS, ROLES, CATALOGOS, REPORTES | LECTURA, ESCRITURA |
| APP_MOVIL, CARRUSEL, CONOCIMIENTO | LECTURA |

---

## 7. Integraciones Cruzadas Implementadas

### 7.1 Correspondencia ↔ Delegaciones
- **`asignarResponsable()` / `crear()`** = reemplazo (solo delegado)
- **`agregarDestinatario()` / `guardarDestinatarios()`** = ambos (original + delegado)
- **`recepcionarYDerivar()`** rastrea delegación en historial
- Provenencia almacenada en entidades (`idDelegacion`, `usuarioOriginal`), leída por `toDTO()`
- Endpoints: `/delegaciones-vigentes`, `/mis-delegaciones`, `/documentos-por-delegacion`

### 7.2 Vacaciones ↔ Delegaciones
- `encargadoAsignacionId` en `SolicitudAusencia` (guarda ID de empleado)
- Al **aprobar ausencia** → backend resuelve asignación activa → auto-crea `DelegacionFuncion`
- Dropdown de encargado con lista completa de empleados

### 7.3 Correspondencia ↔ Tickets
- Generar ticket desde documento (`/generar-ticket`)
- Vincular ticket existente (`/vincular-ticket`)
- Dashboard con documentos pendientes que tienen ticket

### 7.4 Correspondencia ↔ Talento Humano
- Snapshot de firmante (`puesto_firmante`, `unidad_firmante`) en `correspondencia_responsable`
- Derivación institucional por destino: USUARIO, PUESTO, UNIDAD, RESPONSABLE_UNIDAD, JEFE_INMEDIATO

### 7.5 Usuarios ↔ Empleados
- FK `usuarios.empleado_id` → `empleado.id_empleado`
- `GET /usuarios/sin-empleado` para pendientes de vincular
- `PUT /usuarios/{id}/empleado` para vincular/desvincular
- **Nuevo**: crear empleado con usuario vinculado en un solo paso

---

## 8. Funcionalidades por Estado

### ✅ COMPLETADAS Y FUNCIONALES

| Módulo | Estado | Detalle |
|---|---|---|
| Autenticación JWT | ✅ | Login, perfil, refresh, BCrypt |
| Usuarios y roles | ✅ | CRUD completo con permisos |
| Auditoría | ✅ | Log con snapshots JSON, filtros |
| Catálogos | ✅ | Áreas, sistemas, categorías con seed |
| Tickets | ✅ | CRUD, estados, asignación, adjuntos, comentarios |
| Cambios | ✅ | CRUD, aprobación, plan de archivo |
| Versiones | ✅ | CRUD, vinculación a tickets |
| Dashboard general | ✅ | KPIs de tickets y documentos |
| Correspondencia (registro) | ✅ | Crear, editar, anular, tipos, adjuntos |
| Correspondencia (flujos) | ✅ | Recibir, sumillar, derivar, aprobar |
| Correspondencia (sentido) | ✅ | INGRESO/SALIDA con percepción por usuario |
| Correspondencia (bandejas) | ✅ | Unidad, puesto, pendientes |
| Correspondencia (delegaciones) | ✅ | Aplicación de delegaciones al flujo |
| Estructura organizacional | ✅ | Niveles, unidades, organigrama, drag&drop |
| Puestos de trabajo | ✅ | CRUD + perfil completo (6 sub-entidades) |
| Empleados | ✅ | CRUD + expediente completo (4 sub-entidades) |
| Empleados (confidencialidad) | ✅ | §30: 4 niveles de acceso, auditoría §31 |
| Empleados (repositorio archivos) | ✅ | §29: UUID + SHA-256 + mime |
| Empleados (crear usuario) | ✅ | **NUEVO**: opción de crear cuenta vinculada |
| Asignaciones de puesto | ✅ | Asignar, finalizar, historial, jefatura |
| Distributivo | ✅ | Consulta + exportación Excel/PDF |
| Movimientos de personal | ✅ | Workflow completo con ejecución |
| Acciones de personal | ✅ | Workflow borrador → aprobación |
| Vacaciones y permisos | ✅ | Solicitud → jefe → TH, auto-delegación |
| Delegaciones de función | ✅ | CRUD + integración con correspondencia |
| Manual de funciones | ✅ | Lectura en vivo + inline creation + drag&drop + versionado |
| Migración usuarios→empleados | ✅ | §35: idempotente con dry-run |
| Base de conocimiento | ✅ | CRUD artículos |
| App móvil (APK) | ✅ | Upload/download, última versión |
| Reportes TH | ✅ | Dashboard, matriz persona-puesto |
| Notificaciones | ✅ | WebSocket STOMP + persistencia |
| Seguridad (permisos granulares) | ✅ | LECTURA/ESCRITURA por módulo |

### 🔄 EN PROGRESO / RECIENTEMENTE ACTUALIZADO

| Módulo | Estado | Detalle |
|---|---|---|
| Sidebar / Template | ✅ mejorado | Iconos actualizados, colores por sección, subgrupos TH |
| Sentido de documentos | ✅ corregido | Percepción consistente para destinatarios Y responsables |
| Empleado + Usuario | ✅ implementado | Crear empleado con cuenta de usuario vinculada |
| Carrusel | ⚠️ básico | Componente presente, funcionalidad limitada |

### ⏳ PENDIENTES / BACKLOG

| Módulo | Estado | Detalle |
|---|---|---|
| Tests de integración | ⏳ | 48 unit tests (TH solamente), 0 controller tests, 0 frontend tests |
| Tests de correspondencia | ⏳ | Sin cobertura de tests |
| Testcontainers | ⏳ | Declarado en pom.xml pero no utilizado |
| DDL unificado | ⏳ | Módulos correspondencia y TH dependen de migraciones + Hibernate auto-DDL |
| Exportación correspondencia | ⏳ | No tiene exportación a PDF/Excel (solo distributivo la tiene) |
| Mobile-responsive completo | ⏳ | Sidebar responsive existe, formularios no optimizados para móvil |

---

## 9. Infraestructura y Despliegue

| Componente | Estado |
|---|---|
| Docker (compose, Dockerfiles, nginx) | ✅ Archivos en `docker/` |
| Build APK móvil | ✅ `build-apk.ps1` |
| Documentación API (Swagger) | ✅ springdoc-openapi 2.6.0 |
| Docs técnicos | ✅ `docs/API.md`, `docs/ARQUITECTURA.md`, `docs/MIGRACION_PRODUCCION.md` |
| Migraciones de producción | ✅ `migrar-produccion.bat` / `.ps1` |
| Scripts de fix | ✅ `fix-*.sql` para problemas puntuales |

---

## 10. Métricas de Calidad

| Aspecto | Estado | Observación |
|---|---|---|
| Compilación backend | ✅ | `mvn compile` limpio |
| Compilación frontend | ✅ | `ng build` limpio |
| Tests backend | ✅ 48/48 | Solo módulo TH, solo unit tests |
| Tests frontend | ❌ 0 | No existe `.spec.ts` |
| Endpoints documentados | ✅ | Swagger UI habilitado |
| Lint frontend | ⚠️ | Warnings comunes (sweetalert2, stompjs no ESM) |
| TypeScript strict | ⚠️ | Tipos `any` en múltiples componentes |
| Seguridad | ✅ | JWT + RBAC + auditoría + confidencialidad |

---

## 11. Resumen Ejecutivo

**SIGRC es un sistema full-stack completo** con **194 endpoints**, **53 entidades**, **26 módulos frontend** y **6 roles de seguridad**. Los módulos más maduros son:

1. **Talento Humano** (67 endpoints) — estructura, puestos, empleados, asignaciones, movimientos, acciones, vacaciones, delegaciones, manual de funciones, distributivo, dashboard
2. **Correspondencia** (40 endpoints) — ciclo completo con delegaciones, derivación institucional, bandejase, tickets vinculados y **BCC (bandeja compartida controlada)**: vínculo asistente→titular, recepción delegada con idempotencia, lectura personal independiente y restricción de sumilla para secretarías
3. **Seguridad** (43 endpoints) — JWT, RBAC granular, auditoría con snapshots, confidencialidad §30, `SecurityException` → HTTP 403

El sistema está en **estado funcional completo** para producción, con integraciones cruzadas entre módulos (delegaciones ↔ correspondencia ↔ vacaciones, empleados ↔ usuarios, correspondencia ↔ tickets). Las áreas de mejora principal son **tests de integración**, **tests frontend**, y **documentación DDL unificada**.
