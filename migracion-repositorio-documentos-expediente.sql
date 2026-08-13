-- ============================================================================
-- SIGRC · Talento Humano — Repositorio documental del expediente (§29)
-- Tabla: empleado_documento → campos de archivo físico
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- ============================================================================

BEGIN;

ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS nombre_archivo VARCHAR(255);
ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS nombre_fisico VARCHAR(255);
ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS ruta_archivo VARCHAR(500);
ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);
ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS tamano_bytes BIGINT;
ALTER TABLE sigrc.empleado_documento ADD COLUMN IF NOT EXISTS hash_sha256 VARCHAR(64);

COMMENT ON COLUMN sigrc.empleado_documento.nombre_archivo IS
    'Nombre original del archivo adjunto al documento del expediente.';
COMMENT ON COLUMN sigrc.empleado_documento.nombre_fisico IS
    'Nombre físico (UUID) almacenado en disco.';
COMMENT ON COLUMN sigrc.empleado_documento.ruta_archivo IS
    'Ruta completa del archivo en el repositorio documental.';
COMMENT ON COLUMN sigrc.empleado_documento.mime_type IS
    'Tipo MIME del archivo.';
COMMENT ON COLUMN sigrc.empleado_documento.tamano_bytes IS
    'Tamaño del archivo en bytes.';
COMMENT ON COLUMN sigrc.empleado_documento.hash_sha256 IS
    'Hash SHA-256 del archivo para integridad.';

COMMIT;
