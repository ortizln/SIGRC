-- ============================================================================
-- SIGRC · Integración Delegaciones ↔ Correspondencia
-- Agrega campos de trazabilidad de delegación a historial, destinatarios y responsables.
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- ============================================================================

BEGIN;

-- 1. Campos de delegación en correspondencia_historial
ALTER TABLE sigrc.correspondencia_historial
    ADD COLUMN IF NOT EXISTS id_delegacion INTEGER NULL,
    ADD COLUMN IF NOT EXISTS usuario_original INTEGER NULL,
    ADD COLUMN IF NOT EXISTS delegacion_aplicada BOOLEAN DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_hist_delegacion'
    ) THEN
        ALTER TABLE sigrc.correspondencia_historial
            ADD CONSTRAINT fk_hist_delegacion
            FOREIGN KEY (id_delegacion) REFERENCES sigrc.delegacion_funcion(id_delegacion);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_hist_delegacion
    ON sigrc.correspondencia_historial (id_delegacion);

COMMENT ON COLUMN sigrc.correspondencia_historial.id_delegacion IS
    'FK a delegacion_funcion cuando la acción fue resuelta vía delegación.';
COMMENT ON COLUMN sigrc.correspondencia_historial.usuario_original IS
    'ID del usuario original destinatario antes de la resolución por delegación.';
COMMENT ON COLUMN sigrc.correspondencia_historial.delegacion_aplicada IS
    'TRUE si se aplicó delegación al resolver este destino.';

-- 2. Campos de delegación en correspondencia_destinatario
ALTER TABLE sigrc.correspondencia_destinatario
    ADD COLUMN IF NOT EXISTS id_delegacion INTEGER NULL,
    ADD COLUMN IF NOT EXISTS usuario_original INTEGER NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_dest_delegacion'
    ) THEN
        ALTER TABLE sigrc.correspondencia_destinatario
            ADD CONSTRAINT fk_dest_delegacion
            FOREIGN KEY (id_delegacion) REFERENCES sigrc.delegacion_funcion(id_delegacion);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_dest_delegacion
    ON sigrc.correspondencia_destinatario (id_delegacion);

COMMENT ON COLUMN sigrc.correspondencia_destinatario.id_delegacion IS
    'FK a delegacion_funcion cuando este destinatario fue resuelto vía delegación.';
COMMENT ON COLUMN sigrc.correspondencia_destinatario.usuario_original IS
    'ID del usuario original destinatario antes de la resolución por delegación.';

-- 3. Campos de delegación en correspondencia_responsable
ALTER TABLE sigrc.correspondencia_responsable
    ADD COLUMN IF NOT EXISTS id_delegacion INTEGER NULL,
    ADD COLUMN IF NOT EXISTS usuario_original INTEGER NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resp_delegacion'
    ) THEN
        ALTER TABLE sigrc.correspondencia_responsable
            ADD CONSTRAINT fk_resp_delegacion
            FOREIGN KEY (id_delegacion) REFERENCES sigrc.delegacion_funcion(id_delegacion);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_resp_delegacion
    ON sigrc.correspondencia_responsable (id_delegacion);

COMMENT ON COLUMN sigrc.correspondencia_responsable.id_delegacion IS
    'FK a delegacion_funcion cuando este responsable fue asignado vía delegación.';
COMMENT ON COLUMN sigrc.correspondencia_responsable.usuario_original IS
    'ID del usuario original al que se asignó antes de la resolución por delegación.';

-- 4. Retrocompatibilidad: marcar registros existentes
UPDATE sigrc.correspondencia_destinatario
SET usuario_original = id_destinatario
WHERE usuario_original IS NULL;

COMMIT;