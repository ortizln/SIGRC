-- ============================================================
-- SIGRC · Fix: auditoría en esquema antiguo
--
-- La tabla sigrc.auditoria creada por database/01_ddl.sql (esquema
-- viejo) impone dos CHECK constraints incompatibles con el código nuevo:
--   - auditoria_tipo_operacion_check: solo CREATE/READ/UPDATE/DELETE/
--     LOGIN/LOGOUT/EXPORT/APROBAR/RECHAZAR/REASIGNAR
--     (el código nuevo usa: AUTENTICACION, CONSULTA, REGISTRO,
--      MIGRAR_USUARIOS, MIGRAR_CREAR_EMPLEADO, MIGRAR_CREAR_ASIGNACION, ...)
--   - auditoria_resultado_check: solo EXITO/FRACASO
--     (el código nuevo usa: OK, ERROR, FALLIDO, ...)
--
-- Este script elimina esos CHECK constraints antiguos dejando solo
-- validaciones suaves (sin CHECK en tipo_operacion/resultado).
-- Idempotente: si el constraint no existe, continúa sin error.
-- ============================================================

BEGIN;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'sigrc.auditoria'::regclass
          AND contype = 'c'
          AND (
                conname ILIKE '%_resultado%'
                OR conname ILIKE '%_tipo_operacion%'
                OR conname ILIKE '%auditoria%resultado%'
                OR conname ILIKE '%auditoria%tipo%'
          )
    LOOP
        EXECUTE format('ALTER TABLE sigrc.auditoria DROP CONSTRAINT %I', r.conname);
        RAISE NOTICE 'Constraint eliminado: sigrc.auditoria.%', r.conname;
    END LOOP;

    IF NOT FOUND THEN
        RAISE NOTICE 'No se encontraron CHECK constraints de resultado/tipo_operacion en sigrc.auditoria (ya limpio).';
    END IF;
END $$;

-- Verificación: mostrar constraints CHECK restantes en la tabla
DO $$
DECLARE
    listado TEXT := '';
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'sigrc.auditoria'::regclass AND contype = 'c'
    LOOP
        listado := listado || r.conname || ', ';
    END LOOP;
    RAISE NOTICE 'CHECK constraints restantes en sigrc.auditoria: %', NULLIF(listado, '');
END $$;

COMMIT;