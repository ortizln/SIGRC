-- ============================================================================
-- SIGRC · Fix: anchos de columnas de sigrc.auditoria compatibles con backend
--
-- La tabla sigrc.auditoria creada por el esquema antiguo conserva columnas
-- angostas (p. ej. username/accion en VARCHAR(50)) que el backend nuevo
-- desborda (accion "CONSULTAR_EXPEDIENTE", user_agent de navegadores,
-- tabla_afectada, resultados largos, etc.). Hibernate con ddl-auto=update
-- NO ensancha columnas existentes, solo agrega las que faltan.
--
-- Este script ensancha dinámicamente TODAS las columnas de texto/varchar de
-- sigrc.auditoria al ancho objetivo del backend, sin importar cómo se llame
-- la columna en producción (también cubre variantes: session_id/sesion_id,
-- usuario/username, JSONB en datos_*).
--
-- Idempotente: puede ejecutarse varias veces sin error.
-- ============================================================================

BEGIN;

-- 1) datos_anteriores / datos_nuevos: asegurar TEXT (compat JSONB del DDL viejo)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='sigrc' AND table_name='auditoria'
                 AND column_name='datos_anteriores') THEN
        ALTER TABLE sigrc.auditoria ALTER COLUMN datos_anteriores TYPE TEXT USING datos_anteriores::text;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='sigrc' AND table_name='auditoria'
                 AND column_name='datos_nuevos') THEN
        ALTER TABLE sigrc.auditoria ALTER COLUMN datos_nuevos TYPE TEXT USING datos_nuevos::text;
    END IF;
END $$;

-- 2) Ensanchar cada columna varchar/char a su ancho objetivo según la entidad
DO $$
DECLARE
    r RECORD;
    target INTEGER;
BEGIN
    FOR r IN
        SELECT column_name, data_type, character_maximum_length AS cur_len
        FROM information_schema.columns
        WHERE table_schema = 'sigrc' AND table_name = 'auditoria'
          AND data_type IN ('character varying', 'character')
    LOOP
        target := CASE r.column_name
            WHEN 'username'      THEN 100
            WHEN 'usuario'       THEN 100
            WHEN 'accion'        THEN 100
            WHEN 'tipo_operacion' THEN 50
            WHEN 'tabla_afectada' THEN 255
            WHEN 'direccion_ip'  THEN 45
            WHEN 'resultado'     THEN 30
            WHEN 'sesion_id'     THEN 255
            WHEN 'session_id'    THEN 255
            WHEN 'user_agent'    THEN 500
            WHEN 'detalle'       THEN 1000
            ELSE 255
        END;
        IF r.cur_len IS NULL OR r.cur_len < target THEN
            EXECUTE format(
                'ALTER TABLE sigrc.auditoria ALTER COLUMN %I TYPE VARCHAR(%s) USING %I::varchar',
                r.column_name, target, r.column_name);
            RAISE NOTICE 'Ensanchada columna % a VARCHAR(%)', r.column_name, target;
        END IF;
    END LOOP;
END $$;

-- 3) Asegurar sesion_id (el esquema viejo pudo dejar session_id)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='sigrc' AND table_name='auditoria' AND column_name='session_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='sigrc' AND table_name='auditoria' AND column_name='sesion_id') THEN
        ALTER TABLE sigrc.auditoria RENAME COLUMN session_id TO sesion_id;
    END IF;
END $$;

ALTER TABLE sigrc.auditoria ADD COLUMN IF NOT EXISTS sesion_id VARCHAR(255);

COMMIT;
