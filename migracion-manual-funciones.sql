-- ============================================================================
-- SIGRC · Talento Humano — Manual de Funciones digital (§20–21)
-- Tabla: version_manual · Columna nueva: puesto.version_manual_id
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- Si la tabla la creó Hibernate (ddl-auto: update), este script solo agrega
-- columnas faltantes, índices y comentarios.
-- ============================================================================

BEGIN;

-- ─────────────────── version_manual ───────────────────
CREATE TABLE IF NOT EXISTS sigrc.version_manual (
    id_version_manual SERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    version VARCHAR(20) NOT NULL,
    fecha_aprobacion DATE,
    fecha_vigencia DATE,
    documento_id BIGINT,
    estado VARCHAR(20) NOT NULL,
    observaciones VARCHAR(1000),
    creado_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_version_manual_estado ON sigrc.version_manual (estado);
CREATE INDEX IF NOT EXISTS idx_version_manual_vigencia ON sigrc.version_manual (fecha_vigencia);

COMMENT ON TABLE sigrc.version_manual IS
    'Versiones del Manual Orgánico Funcional. Estados: BORRADOR, VIGENTE, DEROGADO. Solo una VIGENTE a la vez.';
COMMENT ON COLUMN sigrc.version_manual.estado IS
    'Estado: BORRADOR, VIGENTE, DEROGADO';

-- ─────────────────── puesto.version_manual_id ───────────────────
ALTER TABLE sigrc.puesto
    ADD COLUMN IF NOT EXISTS version_manual_id INTEGER;

CREATE INDEX IF NOT EXISTS idx_puesto_version_manual ON sigrc.puesto (version_manual_id);

COMMENT ON COLUMN sigrc.puesto.version_manual_id IS
    'Versión del Manual Orgánico Funcional a la que pertenece el puesto/perfil';

COMMIT;
