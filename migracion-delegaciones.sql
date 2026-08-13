-- ============================================================================
-- SIGRC · Talento Humano — Delegaciones de funciones (§25)
-- Tabla: delegacion_funcion
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS sigrc.delegacion_funcion (
    id_delegacion SERIAL PRIMARY KEY,
    asignacion_origen_id INTEGER NOT NULL,
    asignacion_delegada_id INTEGER NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    tipo VARCHAR(30),
    alcance VARCHAR(20),
    documento_respaldo_id BIGINT,
    estado VARCHAR(20) NOT NULL,
    observacion VARCHAR(1000),
    creado_por INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_delegacion_origen ON sigrc.delegacion_funcion (asignacion_origen_id);
CREATE INDEX IF NOT EXISTS idx_delegacion_delegada ON sigrc.delegacion_funcion (asignacion_delegada_id);
CREATE INDEX IF NOT EXISTS idx_delegacion_estado ON sigrc.delegacion_funcion (estado);

COMMENT ON TABLE sigrc.delegacion_funcion IS
    'Delegación de funciones entre asignaciones (asignación origen -> delegada). Cubre vacaciones, encargos y ausencias sin cambiar permisos.';
COMMENT ON COLUMN sigrc.delegacion_funcion.tipo IS
    'Tipo: VACACIONES, PERMISO, LICENCIA, ENCARGO, COMISION, AUSENCIA, OTRO';
COMMENT ON COLUMN sigrc.delegacion_funcion.alcance IS
    'Alcance: TOTAL, PARCIAL';
COMMENT ON COLUMN sigrc.delegacion_funcion.estado IS
    'Estado: ACTIVA, CANCELADA, FINALIZADA';

COMMIT;
