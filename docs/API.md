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

## Auditoría

### GET /auditoria
Lista paginada de registros de auditoría.

**Parámetros:** `pagina`, `tamanio`, `username`, `tabla`, `tipoOperacion`, `desde`, `hasta`

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
