-- Fix: numero_interno con iniciales del usuario creador y secuencia propia
-- Formato: INICIALES-YYYY-SSSSSSS (ej: AO-2026-0000001)
BEGIN;

DROP TRIGGER IF EXISTS trg_generar_numero_correspondencia ON sigrc.correspondencia;

CREATE OR REPLACE FUNCTION sigrc.generar_numero_correspondencia()
RETURNS TRIGGER AS $$
DECLARE
    correlativo INTEGER;
    anio_actual VARCHAR(4);
    iniciales   VARCHAR(4);
BEGIN
    anio_actual := TO_CHAR(CURRENT_DATE, 'YYYY');
    SELECT UPPER(LEFT(nombres, 1) || LEFT(apellidos, 1)) INTO iniciales
    FROM sigrc.usuarios WHERE id_usuario = NEW.creado_por;
    IF iniciales IS NULL OR iniciales = '' THEN
        iniciales := 'XX';
    END IF;
    SELECT COALESCE(MAX(SPLIT_PART(numero_interno, '-', 3)::INTEGER), 0) + 1
    INTO correlativo
    FROM sigrc.correspondencia
    WHERE numero_interno LIKE iniciales || '-' || anio_actual || '-%';
    NEW.numero_interno := iniciales || '-' || anio_actual || '-' || LPAD(correlativo::TEXT, 7, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generar_numero_correspondencia
    BEFORE INSERT ON sigrc.correspondencia
    FOR EACH ROW
    WHEN (NEW.numero_interno IS NULL)
    EXECUTE FUNCTION sigrc.generar_numero_correspondencia();

COMMIT;
