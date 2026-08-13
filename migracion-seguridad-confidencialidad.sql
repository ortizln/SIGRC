-- ============================================================================
-- SIGRC · Talento Humano — Seguridad de información y confidencialidad (§30)
-- Tabla: empleado_documento → nivel_acceso
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- ============================================================================

BEGIN;

ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS nivel_acceso VARCHAR(30);

UPDATE sigrc.empleado_documento
   SET nivel_acceso = CASE
        WHEN confidencial THEN 'CONFIDENCIAL_RRHH'
        ELSE 'PUBLICO_INSTITUCIONAL'
      END
 WHERE nivel_acceso IS NULL;

COMMENT ON TABLE sigrc.empleado_documento IS
    'Documentos del expediente del empleado con clasificación de confidencialidad.';
COMMENT ON COLUMN sigrc.empleado_documento.nivel_acceso IS
    'Clasificación de acceso: PUBLICO_INSTITUCIONAL, INTERNO, CONFIDENCIAL_RRHH, RESTRINGIDO.';
COMMENT ON COLUMN sigrc.empleado_documento.confidencial IS
    'Marcador booleano de confidencialidad (respaldo de nivel_acceso).';

COMMIT;
