-- Fix: datos_anteriores y datos_nuevos en auditoria son VARCHAR(255)
-- pero almacenan JSON que puede exceder ese límite.
BEGIN;

ALTER TABLE sigrc.auditoria
  ALTER COLUMN datos_anteriores TYPE TEXT,
  ALTER COLUMN datos_nuevos TYPE TEXT;

COMMIT;
