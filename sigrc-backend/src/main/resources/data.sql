-- ============================================================
-- SIGRC - Datos iniciales (ejecutado por Spring Boot al iniciar)
-- Solamente funciones, triggers, vistas y datos semilla
-- ============================================================
SET search_path TO sigrc, public;

-- Funciones
CREATE OR REPLACE FUNCTION generar_numero_ticket()
RETURNS TRIGGER AS $$
DECLARE
    correlativo INTEGER;
    prefijo     VARCHAR(8);
BEGIN
    prefijo := 'SIGRC-' || TO_CHAR(CURRENT_DATE, 'YYYYMM');
    SELECT COALESCE(MAX(SPLIT_PART(numero_ticket, '-', 3)::INTEGER), 0) + 1
    INTO correlativo
    FROM tickets
    WHERE numero_ticket LIKE prefijo || '-%';
    NEW.numero_ticket := prefijo || '-' || LPAD(correlativo::TEXT, 5, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generar_codigo_cambio()
RETURNS TRIGGER AS $$
DECLARE
    correlativo INTEGER;
    prefijo     VARCHAR(8);
BEGIN
    prefijo := 'CHG-' || TO_CHAR(CURRENT_DATE, 'YYYYMM');
    SELECT COALESCE(MAX(SPLIT_PART(codigo_cambio, '-', 3)::INTEGER), 0) + 1
    INTO correlativo
    FROM cambios
    WHERE codigo_cambio LIKE prefijo || '-%';
    NEW.codigo_cambio := prefijo || '-' || LPAD(correlativo::TEXT, 5, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers
DROP TRIGGER IF EXISTS trg_generar_numero_ticket ON tickets;
CREATE TRIGGER trg_generar_numero_ticket
    BEFORE INSERT ON tickets
    FOR EACH ROW
    WHEN (NEW.numero_ticket IS NULL)
    EXECUTE FUNCTION generar_numero_ticket();

DROP TRIGGER IF EXISTS trg_generar_codigo_cambio ON cambios;
CREATE TRIGGER trg_generar_codigo_cambio
    BEFORE INSERT ON cambios
    FOR EACH ROW
    WHEN (NEW.codigo_cambio IS NULL)
    EXECUTE FUNCTION generar_codigo_cambio();

DROP TRIGGER IF EXISTS trg_tickets_updated ON tickets;
CREATE TRIGGER trg_tickets_updated
    BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

DROP TRIGGER IF EXISTS trg_cambios_updated ON cambios;
CREATE TRIGGER trg_cambios_updated
    BEFORE UPDATE ON cambios
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- Vistas
CREATE OR REPLACE VIEW v_tickets_activos AS
SELECT
    t.id_ticket, t.numero_ticket, t.tipo, t.estado, t.prioridad,
    t.asunto, t.descripcion, t.fecha_limite, t.fecha_cierre,
    t.creado_en, t.actualizado_en,
    sol.nombres || ' ' || sol.apellidos AS nombre_solicitante,
    a.nombre AS area,
    s.nombre AS sistema,
    cat.nombre AS categoria,
    sub.nombre AS subcategoria,
    res.nombres || ' ' || res.apellidos AS nombre_responsable,
    EXTRACT(EPOCH FROM (COALESCE(t.fecha_cierre, CURRENT_TIMESTAMP) - t.creado_en))/3600 AS horas_transcurridas
FROM tickets t
JOIN usuarios sol      ON t.id_solicitante = sol.id_usuario
JOIN areas a           ON t.id_area = a.id_area
LEFT JOIN sistemas s   ON t.id_sistema = s.id_sistema
LEFT JOIN categorias cat  ON t.id_categoria = cat.id_categoria
LEFT JOIN subcategorias sub ON t.id_subcategoria = sub.id_subcategoria
LEFT JOIN usuarios res ON t.id_responsable = res.id_usuario
WHERE t.activo = TRUE AND t.estado NOT IN ('CERRADO','RECHAZADO');

CREATE OR REPLACE VIEW v_cumplimiento_sla AS
SELECT
    t.tipo, t.prioridad,
    COUNT(*) AS total_tickets,
    SUM(CASE WHEN t.fecha_cierre IS NOT NULL AND t.fecha_cierre <= t.fecha_limite THEN 1 ELSE 0 END) AS dentro_sla,
    ROUND(100.0 * SUM(CASE WHEN t.fecha_cierre IS NOT NULL AND t.fecha_cierre <= t.fecha_limite THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS porcentaje_cumplimiento
FROM tickets t
WHERE t.estado IN ('CERRADO','RESUELTO')
GROUP BY t.tipo, t.prioridad;

-- Datos semilla (solo si están vacíos)
INSERT INTO roles (codigo, nombre, descripcion, activo, creado_en)
SELECT * FROM (VALUES
    ('ADMIN','Administrador','Acceso total al sistema', true, CURRENT_TIMESTAMP),
    ('JEFE_TI','Jefe de TI','Supervision y aprobacion de cambios', true, CURRENT_TIMESTAMP),
    ('TECNICO','Tecnico de Soporte','Atencion y resolucion de tickets', true, CURRENT_TIMESTAMP),
    ('AUDITOR','Auditor','Consulta de auditoria y reportes', true, CURRENT_TIMESTAMP),
    ('SUPERVISOR','Supervisor','Supervision de tickets y reportes', true, CURRENT_TIMESTAMP),
    ('SOLICITANTE','Usuario Solicitante','Creacion y seguimiento de tickets', true, CURRENT_TIMESTAMP)
) AS v
WHERE NOT EXISTS (SELECT 1 FROM roles);

INSERT INTO areas (codigo, nombre, activo, creado_en)
SELECT * FROM (VALUES
    ('SISTEMAS','Sistemas Informaticos', true, CURRENT_TIMESTAMP),
    ('GERENCIA','Gerencia General', true, CURRENT_TIMESTAMP),
    ('ADMIN','Administracion', true, CURRENT_TIMESTAMP),
    ('FINANZAS','Direccion Financiera', true, CURRENT_TIMESTAMP),
    ('OPERACIONES','Operaciones', true, CURRENT_TIMESTAMP),
    ('COMERCIAL','Comercializacion', true, CURRENT_TIMESTAMP),
    ('TECNICA','Direccion Tecnica', true, CURRENT_TIMESTAMP),
    ('RECURSOS_H','Recursos Humanos', true, CURRENT_TIMESTAMP),
    ('ASESORIA_J','Asesoria Juridica', true, CURRENT_TIMESTAMP),
    ('PLANIFICACION','Planificacion y Presupuesto', true, CURRENT_TIMESTAMP)
) AS v
WHERE NOT EXISTS (SELECT 1 FROM areas);
