# SIGRC — Migración de base de datos en Producción

Guía para migrar una base de datos PostgreSQL **en producción** desde el
**esquema antiguo** (tickets, cambios, usuarios, áreas, correspondencia) hacia la
**nueva estructura** (Talento Humano: niveles, unidades, puestos, empleados,
asignaciones y sus módulos asociados).

> **Leer completa antes de ejecutar.** Todos los comandos se ejecutan en el
> servidor que tenga acceso a la BD. Todos los scripts de migración del repositorio
> son **idempotentes** (pueden repetirse sin duplicar datos), salvo los que se
> indican explícitamente.

---

## 0. Requisitos previos

| Requisito | Detalle |
|---|---|
| `psql` / `pg_dump` | Cliente PostgreSQL 16+ (la BD productiva usa `schema sigrc`) |
| Credenciales de BD | Usuario con permiso de `DDL` y `DML` en el esquema `sigrc` (configuración real en `sigrc-backend/src/main/resources/application-prod.yml`) |
| Código actualizado | El backend **nuevo** compilado (`mvn -o package -DskipTests`) y el frontend nuevo |
| Ejecutor | Ventana de terminal del servidor (Linux) o PowerShell (Windows) |

Conectar por defecto (ajustar host/usuario/puerto a tu entorno):

```bash
export PGPASSWORD='***'          # Linux/macOS
$env:PGPASSWORD='***'            # PowerShell
psql -h <host_prod> -U postgres -d sigrc
```

---

## 1. Respaldo completo (OBLIGATORIO)

Nunca ejecutar migraciones sin respaldo verificable. Desde una máquina con
acceso a la BD productiva:

```bash
pg_dump -h <host_prod> -U postgres -d sigrc \
  --format=custom --file=sigrc_backup_$(date +%Y%m%d_%H%M).dump

# Verificar que el respaldo es legible
pg_restore --list sigrc_backup_$(date +%Y%m%d_%H%M).dump | head -5
```

Copiar el `.dump` a un lugar seguro (disco externo / almacenamiento de respaldos).

> **Rollback:** para restaurar en caso de error:
> `dropdb`/`DROP SCHEMA sigrc CASCADE` (según corresponda) y
> `pg_restore -h <host_prod> -U postgres -d sigrc --clean --if-exists sigrc_backup_*.dump`

---

## 2. Crear el esquema nuevo (tablas TH)

El esquema Talento Humano **no** está en `database/01_ddl.sql` (ese archivo es el
esquema antiguo). Las tablas TH las crea automáticamente Hibernate al arrancar el
backend con `spring.jpa.hibernate.ddl-auto: update` (configuración por defecto).

**Forma recomendada (2 opciones):**

- **Opción A — Desplegar el backend nuevo (recomendada):** publicar la versión
  nueva en el servidor, detener el servicio viejo, arrancar el nuevo y esperar a
  que el log muestre `Started SigrcApplication`. Hibernate crea las tablas y
  `data.sql` siembra catálogos, funciones, triggers y la estructura base
  (niveles, unidades, puestos).
- **Opción B — Solo esquema (sin desplegar):** generar el DDL manual desde las
  entidades (`hibernate.hbm2ddl.auto=update` contra una BD vacía y exportar con
  `pg_dump --schema-only`), o escribir el DDL equivalente. *No hay script TH
  completo en el repo; la opción A es la vía soportada.*

### 2.1 Despliegue Docker (Opción A en contenedores)

El backend de producción corre en Docker (`docker/docker-compose.yml`, perfil
`prod`, BD en `192.168.1.43`). Para crear el esquema TH con la opción A:

```bash
# 1) Llevar el código nuevo al servidor donde se construyen las imágenes
#    (git pull o copiar sigrc-backend/).

# 2) Construir y levantar la imagen backend (desde la carpeta del compose)
cd docker
docker compose build backend
docker compose up -d backend

# 3) Verificar el arranque y la creación de tablas
docker logs -f sigrc-backend
#    Buscar: "Started SigrcApplication" y ausencia de errores de BD
```

> El contenedor apunta a la BD por variables de entorno del `.env` del servidor
> (`DATABASE_HOST/PORT/NAME`, `DATABASE_USER`, `DB_PASSWORD`). `ddl-auto: update`
> solo **crea/actualiza** tablas; no toca ni borra datos existentes.

Verificar que las tablas existen tras el arranque:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'sigrc'
  AND table_name IN ('nivel_organizacional','unidad_organizacional','puesto',
                     'empleado','asignacion_puesto','empleado_documento',
                     'delegacion_funcion','movimiento_personal','accion_personal',
                     'solicitud_ausencia','version_manual')
ORDER BY table_name;
```

Las columnas principales del nuevo esquema (para contrastar después de migrar):

```sql
-- usuarios: debe existir la columna empleado_id (la agrega el script de datos)
SELECT column_name FROM information_schema.columns
WHERE table_schema='sigrc' AND table_name='usuarios' AND column_name='empleado_id';
```

---

## 3. Orden de ejecución de los scripts

> **Forma automatizada (recomendada):** usar `migrar-produccion.ps1` (o
> `migrar-produccion.bat` para doble clic) que ejecuta todos los pasos de este
> capítulo en orden, con respaldo previo, detección de errores y verificación
> final. Solo requiere la contraseña de PostgreSQL:
>
> ```powershell
> .\migrar-produccion.ps1          # prod por defecto (192.168.1.43), pide password
> .\migrar-produccion.ps1 -Restore # restaura el último respaldo de .\backups
> ```

Los pasos manuales quedan a continuación por si se prefiere ejecutar cada script
individualmente. Todos los scripts están en la raíz del repositorio. Ejecutar en
este orden. Cada comando usa `--set ON_ERROR_STOP=1` para abortar ante error.

```bash
cd <ruta_del_repositorio>
```

### 3.1 Migración de datos: usuarios → empleados (núcleo)

```bash
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 \
  -f migracion-usuarios-empleados.sql
```

Qué hace (6 pasos, idempotente y **transaccional** — si un paso falla se revierte todo):

1. Agrega `sigrc.usuarios.empleado_id` (FK → `empleado`) si no existe.
2. Crea `unidad_organizacional` a partir de `areas` (reutiliza si coinciden código o nombre).
3. Crea `puesto` a partir de `usuarios.cargo` por unidad del área (sin duplicar).
4. Crea `empleado` desde `usuarios` sin empleado vinculado — identificación provisional `LEG-<idUsuario>`, `tipo_identificacion='LEGACY'`, `tipo_personal='EMPLEADO'`, `estado_laboral='ACTIVO'`.
5. Crea la asignación principal `TITULAR`/`ACTIVA` de cada empleado (fecha inicio = `usuarios.creado_en`; si no hay fecha, `CURRENT_DATE`).
6. Vincula `usuarios.empleado_id`.

> **Nota importante:** las identificaciones generadas son provisionales
> (`LEG-…`). Después de la migración, cada empleado debe completarse en el
> expediente (identificación real, fecha de nacimiento, etc.).

> **Diferencia con el endpoint (`POST /talento-humano/migracion/usuarios`):**
> el script SQL crea empleado **y** asignación para todo usuario con cargo, incluso
> si no tiene área (el puesto queda sin unidad). El endpoint, en cambio, crea el
> empleado pero **no** la asignación si falta área/cargo (resultado `SIN_CARGO`).
> Verificar después de migrar que no queden asignaciones sin unidad:
> ```sql
> SELECT count(*) FROM sigrc.asignacion_puesto
> WHERE unidad_organizacional_id IS NULL;
> ```

Verificación tras 3.1:

```sql
-- Debe existir el mismo nº de empleados que de usuarios activos (o menos si algunos ya estaban)
SELECT
  (SELECT count(*) FROM sigrc.usuarios  WHERE activo)  AS usuarios_activos,
  (SELECT count(*) FROM sigrc.empleado)                AS empleados,
  (SELECT count(*) FROM sigrc.asignacion_puesto
     WHERE estado='ACTIVA' AND es_principal)          AS asignaciones_vigentes,
  (SELECT count(*) FROM sigrc.usuarios WHERE empleado_id IS NULL AND activo) AS usuarios_sin_empleado;
```

### 3.2 Módulos Talento Humano (estructuras adicionales)

Ejecutar en este orden. Los scripts son idempotentes (si Hibernate ya creó las
tablas, solo agregan índices/comentarios/columnas faltantes):

```bash
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-manual-funciones.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-gestion-personal.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-delegaciones.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-seguridad-confidencialidad.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-repositorio-documentos-expediente.sql
```

| Script | Qué agrega |
|---|---|
| `migracion-manual-funciones.sql` | Tabla `version_manual` + `puesto.version_manual_id` (§20-21) |
| `migracion-gestion-personal.sql` | Columnas en `movimiento_personal`, `accion_personal`, `solicitud_ausencia` (flujos de personal) |
| `migracion-delegaciones.sql` | Tabla `delegacion_funcion` (§25) |
| `migracion-seguridad-confidencialidad.sql` | `empleado_documento.nivel_acceso` y clasificación de los documentos existentes (§30) |
| `migracion-repositorio-documentos-expediente.sql` | Campos de archivo físico en `empleado_documento` (§29) |

### 3.3 Correspondencia (migraciones incrementales)

```bash
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-correspondencia.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f migracion-correspondencia-integracion-th.sql
```

| Script | Qué agrega |
|---|---|
| `migracion-correspondencia.sql` | `correspondencia.sentido`, tablas `correspondencia_referencia`, `correspondencia_destinatario`, `correspondencia_responsable`; migra `id_responsable` → tabla de responsables |
| `migracion-correspondencia-integracion-th.sql` | Instantánea de firma en `correspondencia_responsable` (puesto/unidad/asignación del firmante) |

> **Advertencia:** `migracion-correspondencia-destinatario.sql` usa sintaxis
> MySQL (`AUTO_INCREMENT`, `COMMENT`), **NO es válido para PostgreSQL**. No
> ejecutarlo; su contenido ya lo cubre `migracion-correspondencia.sql`.
>
> `migracion-responsables-sumilla.sql` **solo debe ejecutarse** si
> `correspondencia_responsable` ya existía como tabla `@ManyToMany` antigua (PK
> compuesta). Si el backend nuevo creó `correspondencia_responsable` con la
> estructura actual (PK `id` + `sumilla`), **no ejecutar este script** — rompería
> la tabla. Verificar antes:
> ```sql
> SELECT column_name FROM information_schema.columns
> WHERE table_schema='sigrc' AND table_name='correspondencia_responsable'
> ORDER BY ordinal_position;
> ```

### 3.4 Correcciones y reparaciones (fixes)

```bash
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f fix-auditoria-columns.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f fix-auditoria-checks.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f fix-generar-numero-ticket.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f fix-numero-interno-por-usuario.sql
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f arreglar-fk-responsables.sql
```

- `fix-auditoria-columns.sql`: `datos_anteriores`/`datos_nuevos` de `VARCHAR(255)` → `TEXT` (el JSON de auditoría puede exceder 255).
- `fix-auditoria-checks.sql`: **OBLIGATORIO si la BD viene del esquema antiguo.** Elimina los CHECK constraints viejos de `sigrc.auditoria` (`auditoria_resultado_check` = solo `EXITO/FRACASO` y `auditoria_tipo_operacion_check` = solo `CREATE/READ/...`). El código nuevo escribe `resultado='OK'` y `tipo_operacion='AUTENTICACION'/'REGISTRO'/'CONSULTA'`, que los violan y rompen el login (`DataIntegrityViolationException`).
- `fix-generar-numero-ticket.sql`: corrige la función del consecutivo de tickets (prefijo 12 caracteres).
- `fix-numero-interno-por-usuario.sql`: consecutivo de correspondencia por iniciales del creador.
- `arreglar-fk-responsables.sql`: asegura que la FK de `correspondencia_responsable` apunte a `sigrc.usuarios`.

Opcional (solo si hace falta reconstruir responsables desde historial):

```bash
psql -h <host_prod> -U postgres -d sigrc --set ON_ERROR_STOP=1 -f recuperar-responsables.sql
```

> `arreglar-fk-responsables.sql` y `recuperar-responsables.sql` corrigen/recuperan
> datos existentes; se pueden omitir si la FK ya es correcta y no se perdió
> ninguna asignación.

---

## 4. Verificación integral

### 4.1 Consistencia de la migración

```sql
-- 1) Todos los usuarios activos deben tener empleado (salvo los que a propósito no)
SELECT username, empleado_id, cargo
FROM sigrc.usuarios WHERE activo AND empleado_id IS NULL;

-- 2) No deben existir asignaciones huérfanas
SELECT count(*) FROM sigrc.asignacion_puesto ap
LEFT JOIN sigrc.empleado e ON e.id_empleado = ap.empleado_id
WHERE e.id_empleado IS NULL;

-- 3) Cada empleado migrado debe tener su asignación principal
SELECT count(*) FROM sigrc.empleado e
LEFT JOIN sigrc.asignacion_puesto ap
  ON ap.empleado_id = e.id_empleado AND ap.es_principal AND ap.estado='ACTIVA'
WHERE e.activo AND ap.id_asignacion IS NULL;

-- 4) Resumen
SELECT 'empleados' AS entidad, count(*) FROM sigrc.empleado
UNION ALL SELECT 'asignaciones', count(*) FROM sigrc.asignacion_puesto
UNION ALL SELECT 'unidades',     count(*) FROM sigrc.unidad_organizacional
UNION ALL SELECT 'puestos',      count(*) FROM sigrc.puesto;
```

### 4.2 Prueba funcional en la aplicación

Con la app nueva desplegada:

1. **Login** de un usuario migrado → en `/auth/me` debe devolver `idEmpleado`,
   `empleadoNombre`, `puestoActual` y `unidadActual` poblados.
2. **Organigrama** (`GET /talento-humano/organigrama`) → unidades/puestos creados
   con plazas y vacantes.
3. **Expediente** de un empleado migrado → datos presentes, identificación
   provisional `LEG-…` visible y pendiente de completar.
4. **Auditoría** (`GET /auditoria`) → registros de la migración (`MIGRAR_*` si se
   usó el endpoint, o los eventos de creación de empleado/puesto si aplican).

---

## 5. Rollback

Si algo falla:

1. Detener la app.
2. Restaurar el respaldo:

```bash
pg_restore -h <host_prod> -U postgres -d sigrc --clean --if-exists \
  sigrc_backup_<fecha>.dump
```

3. Reiniciar con la versión anterior.

> La migración **nunca elimina** los campos antiguos `usuario.area`/`usuario.cargo`
> ni datos de autenticación. El rollback no afecta a tickets, cambios ni
> correspondencia existentes.

---

## 6. Comprobaciones de seguridad y datos

- **No** se almacenan contraseñas en los scripts; los scripts no tocan
  `usuarios.password_hash`.
- Los triggers/funciones que siembra `data.sql` se recrean en cada arranque
  (`CREATE OR REPLACE` + `DROP TRIGGER IF EXISTS`), por lo que arrancar la app no
  genera duplicados.
- Los scripts usan `IF NOT EXISTS` / `ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS`
  para no duplicar datos en re-ejecuciones.
- `migracion-usuarios-empleados.sql` es **transaccional**: envuelto en
  `BEGIN; … COMMIT;` con `--set ON_ERROR_STOP=1`. Si un paso falla, psql aborta y
  la transacción se revierte por completo (no quedan datos a medias).

---

## 7. Resumen ejecutivo (checklist)

- [ ] Respaldo `pg_dump` verificado y guardado fuera del servidor (lo genera el `.ps1` en `.\backups`).
- [ ] Backend nuevo desplegado y arrancado (esquema TH creado por Hibernate) — ver §2.1 (Docker) si aplica.
- [ ] Verificado que existen las tablas TH (`empleado`, `puesto`, `unidad_organizacional`, `asignacion_puesto`, `nivel_organizacional`).
- [ ] `migracion-usuarios-empleados.sql` ejecutado sin errores (paso 3.1 / del `.ps1`).
- [ ] Scripts TH (§3.2) ejecutados.
- [ ] Scripts de correspondencia (§3.3) ejecutados (respetando la advertencia de `migracion-responsables-sumilla.sql`).
- [ ] Fixes (§3.4) ejecutados.
- [ ] Verificaciones §4 OK (empleados vinculados, asignaciones vigentes, `/auth/me` con puesto/unidad).
- [ ] Expedientes de empleados migrados con identificación provisional marcados para completar.
