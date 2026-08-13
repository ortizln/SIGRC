-- ============================================================
-- SIGRC - Migración: usuarios.cargo / usuarios.id_area
--            -> Empleados, Puestos, Unidades y Asignaciones
--
-- Idempotente: puede ejecutarse varias veces sin duplicar datos.
--  1) Agrega la columna sigrc.usuarios.empleado_id (FK -> empleado)
--  2) Crea unidades organizacionales a partir de areas (sin duplicar)
--  3) Crea puestos a partir de usuarios.cargo (por unidad)
--  4) Crea empleados a partir de usuarios (identificacion provisional)
--  5) Crea la asignación principal ACTIVA de cada empleado
--  6) Vincula usuarios.empleado_id
--
-- Transaccional: si cualquier paso falla se revierte todo.
-- ============================================================

BEGIN;

-- 1) Columna empleado_id en usuarios
ALTER TABLE sigrc.usuarios
  ADD COLUMN IF NOT EXISTS empleado_id INTEGER;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    WHERE tc.table_schema = 'sigrc' AND tc.table_name = 'usuarios'
      AND tc.constraint_type = 'FOREIGN KEY' AND tc.constraint_name = 'fk_usuarios_empleado'
  ) THEN
    ALTER TABLE sigrc.usuarios
      ADD CONSTRAINT fk_usuarios_empleado
      FOREIGN KEY (empleado_id) REFERENCES sigrc.empleado(id_empleado);
  END IF;
END $$;

-- 2) Unidades organizacionales desde areas
-- Se reutiliza la unidad existente si coincide el codigo o el nombre.
INSERT INTO sigrc.unidad_organizacional
    (codigo, nombre, tipo_unidad, nivel_organizacional_id, orden, activo, fecha_creacion)
SELECT DISTINCT ON (a.codigo)
    a.codigo,
    a.nombre,
    'UNIDAD',
    (SELECT id_nivel FROM sigrc.nivel_organizacional WHERE codigo = 'OPERATIVO' LIMIT 1),
    100 + ROW_NUMBER() OVER (ORDER BY a.codigo),
    TRUE,
    CURRENT_TIMESTAMP
FROM sigrc.areas a
WHERE NOT EXISTS (
    SELECT 1 FROM sigrc.unidad_organizacional u
    WHERE u.codigo = a.codigo OR LOWER(u.nombre) = LOWER(a.nombre)
);

-- 3) Puestos a partir de usuarios.cargo (por unidad de su area)
INSERT INTO sigrc.puesto
    (codigo, nombre, unidad_organizacional_id, es_jefatura, es_responsable_unidad,
     numero_plazas, activo, vigente_desde, version, creado_en)
SELECT DISTINCT ON (LOWER(TRIM(u.cargo)), COALESCE(u.id_area, 0))
    'MIG-' || LPAD((COALESCE((SELECT MAX(p.id_puesto) FROM sigrc.puesto p), 0)::INTEGER
                  + ROW_NUMBER() OVER (ORDER BY u.id_usuario)::INTEGER)::TEXT, 6, '0'),
    TRIM(u.cargo),
    COALESCE((SELECT id_unidad FROM sigrc.unidad_organizacional WHERE codigo = a.codigo LIMIT 1),
             (SELECT id_unidad FROM sigrc.unidad_organizacional WHERE LOWER(nombre) = LOWER(a.nombre) LIMIT 1)),
    FALSE,
    FALSE,
    1,
    TRUE,
    CURRENT_DATE,
    1,
    CURRENT_TIMESTAMP
FROM sigrc.usuarios u
LEFT JOIN sigrc.areas a ON a.id_area = u.id_area
WHERE u.cargo IS NOT NULL AND LENGTH(TRIM(u.cargo)) > 0
  AND NOT EXISTS (
    SELECT 1 FROM sigrc.puesto p
    WHERE LOWER(p.nombre) = LOWER(TRIM(u.cargo))
      AND p.unidad_organizacional_id IS NOT DISTINCT FROM COALESCE(
            (SELECT id_unidad FROM sigrc.unidad_organizacional WHERE codigo = a.codigo LIMIT 1),
            (SELECT id_unidad FROM sigrc.unidad_organizacional WHERE LOWER(nombre) = LOWER(a.nombre) LIMIT 1))
);

-- 4) Empleados desde usuarios (sin empleado vinculado)
-- Identificacion provisional basada en el username (debe corregirse luego en el expediente).
INSERT INTO sigrc.empleado
    (tipo_identificacion, identificacion, nombres, apellidos,
     correo_institucional, tipo_personal, estado_laboral, activo, created_at, updated_at)
SELECT
    'LEGACY',
    'LEG-' || u.id_usuario,
    u.nombres,
    u.apellidos,
    u.email,
    'EMPLEADO',
    'ACTIVO',
    u.activo,
    COALESCE(u.creado_en, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP
FROM sigrc.usuarios u
WHERE u.empleado_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM sigrc.empleado e WHERE e.identificacion = 'LEG-' || u.id_usuario);

-- 5) Asignaciones principales activas para los empleados recien creados
INSERT INTO sigrc.asignacion_puesto
    (empleado_id, puesto_id, unidad_organizacional_id, tipo_asignacion,
     fecha_inicio, es_principal, estado, observacion, created_at)
SELECT
    e.id_empleado,
    (SELECT p.id_puesto FROM sigrc.puesto p
      WHERE LOWER(p.nombre) = LOWER(TRIM(u.cargo))
        AND p.unidad_organizacional_id IS NOT DISTINCT FROM
             COALESCE((SELECT id_unidad FROM sigrc.unidad_organizacional WHERE codigo = a.codigo LIMIT 1),
                      (SELECT id_unidad FROM sigrc.unidad_organizacional WHERE LOWER(nombre) = LOWER(a.nombre) LIMIT 1))
      LIMIT 1),
    COALESCE((SELECT id_unidad FROM sigrc.unidad_organizacional WHERE codigo = a.codigo LIMIT 1),
             (SELECT id_unidad FROM sigrc.unidad_organizacional WHERE LOWER(nombre) = LOWER(a.nombre) LIMIT 1)),
    'TITULAR',
    COALESCE(u.creado_en::date, CURRENT_DATE),
    TRUE,
    'ACTIVA',
    'Migrado desde datos legados de usuarios',
    CURRENT_TIMESTAMP
FROM sigrc.empleado e
JOIN sigrc.usuarios u ON u.id_usuario = REPLACE(e.identificacion, 'LEG-', '')::INTEGER
LEFT JOIN sigrc.areas a ON a.id_area = u.id_area
WHERE u.empleado_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM sigrc.asignacion_puesto ap WHERE ap.empleado_id = e.id_empleado);

-- 6) Vincular usuarios.empleado_id
UPDATE sigrc.usuarios u
SET empleado_id = e.id_empleado
FROM sigrc.empleado e
WHERE e.identificacion = 'LEG-' || u.id_usuario
  AND u.empleado_id IS NULL;

COMMIT;
