-- ============================================================================
-- SIGRC · Talento Humano — Sprint 7: Gestión de personal
-- Tablas: movimiento_personal, accion_personal, solicitud_ausencia
-- Idempotente: se puede ejecutar múltiples veces sin error.
-- NOTA: si las tablas se crearon por Hibernate (ddl-auto: update), este script
--       solo agrega índices y comentarios; las columnas nuevas se crean con IF NOT EXISTS.
-- ============================================================================

BEGIN;

-- ─────────────────── movimiento_personal ───────────────────
ALTER TABLE sigrc.movimiento_personal
    ADD COLUMN IF NOT EXISTS documento_respaldo_id INTEGER;
ALTER TABLE sigrc.movimiento_personal
    ADD COLUMN IF NOT EXISTS aprobado_por INTEGER;

CREATE INDEX IF NOT EXISTS idx_movimiento_empleado ON sigrc.movimiento_personal (empleado_id);
CREATE INDEX IF NOT EXISTS idx_movimiento_estado ON sigrc.movimiento_personal (estado);

COMMENT ON TABLE sigrc.movimiento_personal IS
    'Movimientos de personal: INGRESO, NOMBRAMIENTO, TRASLADO, ENCARGO, SUBROGACION, LICENCIA, VACACIONES, REINTEGRO, DESVINCULACION, etc.';
COMMENT ON COLUMN sigrc.movimiento_personal.tipo_movimiento IS
    'Tipo de movimiento según plan: INGRESO, NOMBRAMIENTO, CONTRATACION, TRASLADO, TRASPASO, CAMBIO_ADMINISTRATIVO, ENCARGO, SUBROGACION, COMISION_SERVICIOS, LICENCIA, VACACIONES, REINTEGRO, DESVINCULACION, JUBILACION, SUPRESION_PUESTO, OTRO';
COMMENT ON COLUMN sigrc.movimiento_personal.estado IS
    'Estado: BORRADOR, PENDIENTE, APROBADA, RECHAZADA, ANULADA';

-- ─────────────────── accion_personal ───────────────────
ALTER TABLE sigrc.accion_personal
    ADD COLUMN IF NOT EXISTS documento_id INTEGER;
ALTER TABLE sigrc.accion_personal
    ADD COLUMN IF NOT EXISTS revisado_por INTEGER;
ALTER TABLE sigrc.accion_personal
    ADD COLUMN IF NOT EXISTS aprobado_por INTEGER;

CREATE INDEX IF NOT EXISTS idx_accion_empleado ON sigrc.accion_personal (empleado_id);
CREATE INDEX IF NOT EXISTS idx_accion_estado ON sigrc.accion_personal (estado);

COMMENT ON TABLE sigrc.accion_personal IS
    'Acciones de personal elaboradas por Talento Humano';
COMMENT ON COLUMN sigrc.accion_personal.estado IS
    'Estado: BORRADOR, EN_REVISION, APROBADA, RECHAZADA, ANULADA';

-- ─────────────────── solicitud_ausencia ───────────────────
ALTER TABLE sigrc.solicitud_ausencia
    ADD COLUMN IF NOT EXISTS dias INTEGER;
ALTER TABLE sigrc.solicitud_ausencia
    ADD COLUMN IF NOT EXISTS horas INTEGER;
ALTER TABLE sigrc.solicitud_ausencia
    ADD COLUMN IF NOT EXISTS documento_respaldo_id INTEGER;
ALTER TABLE sigrc.solicitud_ausencia
    ADD COLUMN IF NOT EXISTS jefe_aprobador_id INTEGER;
ALTER TABLE sigrc.solicitud_ausencia
    ADD COLUMN IF NOT EXISTS th_aprobador_id INTEGER;

CREATE INDEX IF NOT EXISTS idx_ausencia_empleado ON sigrc.solicitud_ausencia (empleado_id);
CREATE INDEX IF NOT EXISTS idx_ausencia_estado ON sigrc.solicitud_ausencia (estado);

COMMENT ON TABLE sigrc.solicitud_ausencia IS
    'Solicitudes de vacaciones, permisos, licencias y otros. Flujo: funcionario -> jefe inmediato -> Talento Humano';
COMMENT ON COLUMN sigrc.solicitud_ausencia.tipo IS
    'Tipo: VACACION, PERMISO, LICENCIA, CALAMIDAD, ENFERMEDAD, MATERNIDAD, PATERNIDAD, COMISION, OTRO';
COMMENT ON COLUMN sigrc.solicitud_ausencia.estado IS
    'Estado: PENDIENTE_JEFE, PENDIENTE_TH, APROBADA, RECHAZADA, ANULADA';

COMMIT;
