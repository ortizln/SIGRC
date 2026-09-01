-- ============================================================
-- Migración: Integración Vacaciones ↔ Delegaciones
-- Agrega campo encargado_asignacion_id a solicitud_ausencia
-- para designar encargado temporal al solicitar vacaciones/permisos.
-- Al aprobarse la solicitud, se crea automáticamente la delegación.
-- ============================================================

-- 1. Campo encargado_asignacion_id en solicitud_ausencia
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigrc' AND table_name = 'solicitud_ausencia'
          AND column_name = 'encargado_asignacion_id'
    ) THEN
        ALTER TABLE sigrc.solicitud_ausencia
            ADD COLUMN encargado_asignacion_id INTEGER;

        COMMENT ON COLUMN sigrc.solicitud_ausencia.encargado_asignacion_id
            IS 'ID de la asignación del encargado temporal (FK lógica a asignacion_puesto). Al aprobarse, se crea delegación automática.';

        ALTER TABLE sigrc.solicitud_ausencia
            ADD CONSTRAINT fk_solicitud_ausencia_encargado
            FOREIGN KEY (encargado_asignacion_id)
            REFERENCES sigrc.asignacion_puesto(id_asignacion)
            ON DELETE SET NULL;

        CREATE INDEX IF NOT EXISTS idx_solicitud_ausencia_encargado
            ON sigrc.solicitud_ausencia(encargado_asignacion_id);

        RAISE NOTICE 'Columna encargado_asignacion_id agregada a solicitud_ausencia';
    ELSE
        RAISE NOTICE 'Columna encargado_asignacion_id ya existe en solicitud_ausencia';
    END IF;
END $$;
