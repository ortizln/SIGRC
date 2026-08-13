# SIGRC - API REST Documentación

## Base URL

```
Desarrollo: http://localhost:8080/api/v1
Producción: https://sigrc.epmapa.gob.ec/api/v1
```

## Autenticación

### POST /auth/login
Autentica un usuario y devuelve token JWT.

**Request:**
```json
{ "username": "string", "password": "string" }
```
**Response:**
```json
{
  "token": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tipo": "Bearer",
  "expiracion": "2025-01-01T00:00:00",
  "usuario": { ... }
}
```

## Tickets

### GET /tickets
Lista paginada de tickets con filtros.

**Parámetros:** `pagina`, `tamanio`, `estado`, `tipo`, `prioridad`, `idSolicitante`, `idResponsable`, `idArea`, `idSistema`, `texto`

### GET /tickets/{id}
Obtiene un ticket por ID.

### POST /tickets
Crea un nuevo ticket.

**Request:**
```json
{
  "tipo": "INCIDENTE|REQUERIMIENTO|MEJORA|CAMBIO|CONSULTA|PROBLEMA",
  "prioridad": "CRITICA|ALTA|MEDIA|BAJA",
  "idSolicitante": 1,
  "idArea": 1,
  "idSistema": 1,
  "idCategoria": 1,
  "idSubcategoria": 1,
  "asunto": "string",
  "descripcion": "string",
  "impacto": "EXTENSIVO|MODERADO|MENOR|LIMITADO",
  "urgencia": "INMEDIATA|ALTA|MEDIA|BAJA",
  "origen": "SISTEMA|CORREO|TELEFONO|PRESENCIAL|REUNION|OTRO"
}
```

### PATCH /tickets/{id}/estado
Actualiza el estado de un ticket.

### PATCH /tickets/{id}/asignar
Asigna un responsable a un ticket.

### GET /tickets/{id}/comentarios
Obtiene los comentarios de un ticket.

### POST /tickets/{id}/comentarios
Agrega un comentario a un ticket.

## Dashboard

### GET /dashboard
Obtiene indicadores completos del dashboard.

**Response:** DashboardDTO con estadísticas, gráficos y tendencias.

## Cambios

### GET /cambios
Lista todas las solicitudes de cambio.

### GET /cambios/{id}
Obtiene un cambio por ID.

### POST /cambios
Crea una solicitud de cambio.

### PATCH /cambios/{id}/aprobar
Aprueba un cambio (solo ADMIN/JEFE_TI).

## Módulo de Talento Humano

Base: `/talento-humano`. Control de acceso: autenticación JWT obligatoria; mutaciones de estructura/empleados/gestión con `hasRole('ADMIN')` (o permiso del módulo `TALENTO_HUMANO`); expediente protegido por §30 (solo ADMIN, usuarios con permiso TH o el propio empleado, con documentos confidenciales ocultos en auto-consulta). Operaciones sensibles registradas en auditoría (§31).

### Estructura organizacional
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/niveles-organizacionales` | Lista niveles organizacionales |
| POST | `/niveles-organizacionales` | Crea nivel |
| PUT | `/niveles-organizacionales/{id}` | Actualiza nivel |
| DELETE | `/niveles-organizacionales/{id}` | Desactiva nivel |
| GET | `/unidades` | Lista unidades |
| GET | `/unidades/{id}` | Unidad por ID |
| POST | `/unidades` | Crea unidad |
| PUT | `/unidades/{id}` | Actualiza unidad |
| PUT | `/unidades/{id}/responsable` | Asigna responsable de unidad |
| DELETE | `/unidades/{id}` | Desactiva unidad |
| GET | `/organigrama` | Árbol del organigrama dinámico. Cada nodo: `idUnidad`, `codigo`, `nombre`, `sigla`, `tipoUnidad`, `nivelNombre`, `orden`, `activo`, `responsable`, `puestoResponsable`, `plazas`, `plazasOcupadas`, `vacantes`, `hijos` |

### Puestos y perfil del puesto
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/puestos` | Lista puestos activos |
| GET | `/puestos/{id}/perfil` | Puesto con perfil completo (funciones, formaciones, experiencias, capacitaciones, productos, interfaces) |
| POST | `/puestos` | Crea puesto |
| PUT | `/puestos/{id}` | Actualiza puesto |
| DELETE | `/puestos/{id}` | Desactiva puesto |

### Empleados y expediente
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/empleados` | Lista empleados activos |
| GET | `/empleados/{id}/expediente` | Expediente completo (formación, experiencia, capacitaciones, documentos) con control de acceso y confidencialidad |
| POST | `/empleados/{id}/documentos/{idDocumento}/archivo` | Sube el archivo físico de un documento del expediente (ADMIN o permiso TH de escritura) |
| GET | `/empleados/{id}/documentos/{idDocumento}/descargar` | Descarga el archivo; confidenciales solo ADMIN/TH (audita `DESCARGAR_DOCUMENTO_CONFIDENCIAL`) |
| POST | `/empleados` | Crea empleado con expediente |
| PUT | `/empleados/{id}` | Actualiza empleado y expediente |
| DELETE | `/empleados/{id}` | Desactiva empleado (desvinculación auditada) |
| GET | `/mi-expediente` | Auto-consulta del expediente del empleado vinculado al usuario (§30; documentos confidenciales ocultos para no ADMIN/TH) |

### Migración usuarios → empleados (§35)
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/migracion/usuarios` | Fases 1-5: vincula cada usuario activo sin empleado a un `empleado` (identificación sintética `MIG-…`) y crea su asignación vigente `TITULAR` desde `área + cargo`, reutilizando unidades/puestos existentes por nombre normalizado. Idempotente (solo crea lo que falta), auditable, nunca borra ni retira los campos antiguos `usuario.area`/`usuario.cargo`. Cuerpo `{"dryRun": bool}` (true = solo reporta, no persiste). Solo ADMIN |

Respuesta: `{ dryRun, usuariosProcesados, empleadosCreados, asignacionesCreadas, unidadesCreadas, puestosCreados, yaVinculados, conErrores, detalles: [{ idUsuario, username, resultado, detalle, idEmpleado, idAsignacion }] }`. Resultado por usuario: `OK`, `YA_ASIGNADO`, `SIN_CARGO`, `ERROR`.

Documentos del expediente (`empleado_documento`): `tipo` (IDENTIFICACION, CONTRATO, NOMBRAMIENTO, ACCION_PERSONAL, TITULO, CERTIFICADO, CAPACITACION, EXPERIENCIA, EVALUACION, VACACION, PERMISO, LICENCIA, INFORME, DESVINCULACION, OTRO), `confidencial` y `nivelAcceso` (PUBLICO_INSTITUCIONAL, INTERNO, CONFIDENCIAL_RRHH, RESTRINGIDO).

### Asignaciones y jefatura
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/asignaciones?idEmpleado=` | Asignaciones de un empleado |
| GET | `/asignaciones/actual?idEmpleado=` | Asignación principal ACTIVA |
| POST | `/asignaciones` | Asigna puesto (cierra la anterior conservando historial) |
| POST | `/asignaciones/{id}/finalizar` | Finaliza asignación |
| GET | `/jefatura/{idEmpleado}` | Jefe inmediato por estructura |

### Manual de funciones digital (§20-21)
Base: `/talento-humano/manual-funciones`

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/` | Estructura del manual vigente (Direcciones → Unidades → Puestos con ficha completa) |
| GET | `/versiones` | Historial de versiones del manual |
| POST | `/versiones` | Crea versión (BORRADOR) |
| PUT | `/versiones/{id}` | Actualiza versión |
| POST | `/versiones/{id}/aprobar` | Aprueba (VIGENTE; deroga las demás, solo ADMIN) |
| POST | `/versiones/{id}/derogar` | Deroga (solo ADMIN) |

### Delegaciones (§25)
Base: `/talento-humano/delegaciones`

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/` | Lista delegaciones |
| POST | `/` | Crea delegación entre asignaciones (valida solapamiento de ACTIVA) |
| POST | `/{id}/cancelar` | Cancela |
| POST | `/{id}/finalizar` | Finaliza |

La derivación documental resuelve automáticamente la delegación activa del destinatario (`CorrespondenciaService`).

### Gestión de personal (movimientos, acciones, ausencias)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET/POST | `/movimientos` · `/acciones-personal` · `/ausencias` | Listar y crear |
| PUT | `/movimientos/{id}` · `/acciones-personal/{id}` | Actualizar |
| POST | `/movimientos/{id}/enviar` · `/aprobar` · `/rechazar` · `/anular` · `/ejecutar` | Flujo de aprobación |
| POST | `/acciones-personal/{id}/enviar-revision` · `/aprobar` · `/rechazar` · `/anular` | Flujo de acciones |
| POST | `/ausencias/{id}/aprobar-jefe` · `/aprobar-th` · `/rechazar` · `/anular` | Flujo de vacaciones/permisos/licencias |

### Reportes y control
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/distributivo` | Distributivo con filtros (`idUnidad`, `idPuesto`, `estado`, `tipoPersonal`) |
| GET | `/distributivo/exportar?formato=excel\|pdf` | Exportación con los mismos filtros |
| GET | `/dashboard` | KPIs de Talento Humano (activos, puestos, vacantes, ausencias, capacitaciones, movimientos y agrupaciones) |
| GET | `/matriz-persona-puesto/{idEmpleado}` | Matriz persona-puesto (instrucción, formación, experiencia, capacitación) |

## Correspondencia (módulo documental)

Base: `/correspondencia`

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/` | Listado con filtros (bandejas "Mis documentos") |
| GET | `/bandeja-unidad` | Documentos de la unidad (incluye hijas) |
| GET | `/bandeja-puesto` | Documentos de los ocupantes del puesto del usuario (§24) |
| GET | `/pendientes` | Documentos pendientes de atención del usuario (§24) |
| POST | `/{id}/derivar-institucional` | Derivación con delegaciones activas aplicadas (§25) |
| POST | `/{id}/recepcion` · `/recibir` · `/leido` | Recepción y lectura |
| POST | `/{id}/respuesta` | Registro de respuesta con captura de firma institucional |
| GET | `/{id}/adjuntos` · `POST` · `/{id}/adjuntos/{idAdjunto}/descargar` · `DELETE` | Gestión de adjuntos |
| GET | `/{id}/historial` · `/{id}/tickets` | Historial y tickets vinculados |
| POST | `/{id}/generar-ticket` · `/vincular-ticket` | Integración con tickets |
| GET | `/dashboard` · `/tipos-documento` | Indicadores y catálogo |

## Auditoría (§31)

### GET /auditoria
Lista paginada de registros de auditoría.

**Parámetros:** `pagina`, `tamanio`, `username`, `tabla`, `tipoOperacion`, `desde`, `hasta`

**Eventos de Talento Humano registrados:** `LOGIN`, `CREAR_EMPLEADO`, `MODIFICAR_EMPLEADO`, `DESVINCULAR_EMPLEADO`, `CONSULTAR_EXPEDIENTE`, `VER_DOCUMENTO_CONFIDENCIAL`, `SUBIR_DOCUMENTO_EXPEDIENTE`, `DESCARGAR_DOCUMENTO_EXPEDIENTE`, `DESCARGAR_DOCUMENTO_CONFIDENCIAL`, `CREAR_PUESTO`, `MODIFICAR_PUESTO`, `ASIGNAR_PUESTO`, `FINALIZAR_ASIGNACION`, `CREAR_MOVIMIENTO`, `APROBAR_MOVIMIENTO`, `CAMBIAR_ROL`, `CAMBIAR_PERMISO`, `MIGRAR_USUARIOS`, `MIGRAR_CREAR_EMPLEADO`, `MIGRAR_CREAR_ASIGNACION`. No se almacenan contraseñas, tokens ni secretos.

## Catálogos

### GET /catalogos/areas
Lista de áreas activas.

### GET /catalogos/sistemas
Lista de sistemas activos.

### GET /catalogos/categorias
Lista de categorías activas.

### GET /catalogos/subcategorias/{idCategoria}
Lista de subcategorías por categoría.

## Usuarios

### GET /usuarios
Lista de usuarios activos.

### GET /usuarios/{id}
Obtiene un usuario por ID.

## Códigos de Estado

| Código | Descripción |
|--------|-------------|
| 200 | OK |
| 201 | Creado |
| 400 | Error de validación |
| 401 | No autenticado |
| 403 | No autorizado |
| 404 | No encontrado |
| 500 | Error interno |

## Estados de Ticket

| Valor | Display |
|-------|---------|
| NUEVO | Nuevo |
| ASIGNADO | Asignado |
| EN_ANALISIS | En Análisis |
| EN_DESARROLLO | En Desarrollo |
| EN_PRUEBAS | En Pruebas |
| PENDIENTE_USUARIO | Pendiente Usuario |
| RESUELTO | Resuelto |
| CERRADO | Cerrado |
| RECHAZADO | Rechazado |
