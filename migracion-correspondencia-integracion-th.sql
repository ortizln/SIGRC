-- ============================================================================
-- SIGRC · Integración Talento Humano ↔ Correspondencia (Sprint 6)
-- Instantánea de firma institucional en correspondencia_responsable
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- ============================================================================

BEGIN;

-- Columnas de la instantánea de firma en correspondencia_responsable
ALTER TABLE sigrc.correspondencia_responsable
    ADD COLUMN IF NOT EXISTS puesto_firmante  VARCHAR(200);
ALTER TABLE sigrc.correspondencia_responsable
    ADD COLUMN IF NOT EXISTS unidad_firmante  VARCHAR(200);
ALTER TABLE sigrc.correspondencia_responsable
    ADD COLUMN IF NOT EXISTS asignacion_id    INTEGER;

COMMENT ON COLUMN sigrc.correspondencia_responsable.puesto_firmante IS
    'Instantánea del puesto vigente del firmante al momento de la sumilla';
COMMENT ON COLUMN sigrc.correspondencia_responsable.unidad_firmante IS
    'Instantánea de la unidad vigente del firmante al momento de la sumilla';
COMMENT ON COLUMN sigrc.correspondencia_responsable.asignacion_id IS
    'Asignación de puesto vigente capturada al momento de la sumilla';

COMMIT;
