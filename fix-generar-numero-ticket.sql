-- Fix: generar_numero_ticket() tiene prefijo VARCHAR(8) pero el valor
-- 'SIGRC-YYYYMM' tiene 12 caracteres. Se reemplaza con VARCHAR(20).
BEGIN;

DROP TRIGGER IF EXISTS trg_generar_numero_ticket ON sigrc.tickets;

CREATE OR REPLACE FUNCTION sigrc.generar_numero_ticket()
RETURNS TRIGGER AS $$
DECLARE
    correlativo INTEGER;
    prefijo     VARCHAR(20);
BEGIN
    prefijo := 'TK-' || TO_CHAR(CURRENT_DATE, 'YYYYMM');
    SELECT COALESCE(MAX(SPLIT_PART(numero_ticket, '-', 3)::INTEGER), 0) + 1
    INTO correlativo
    FROM sigrc.tickets
    WHERE numero_ticket LIKE prefijo || '-%';
    NEW.numero_ticket := prefijo || '-' || LPAD(correlativo::TEXT, 5, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generar_numero_ticket
    BEFORE INSERT ON sigrc.tickets
    FOR EACH ROW
    WHEN (NEW.numero_ticket IS NULL)
    EXECUTE FUNCTION sigrc.generar_numero_ticket();

COMMIT;
