-- ============================================================
-- SIGRC - Sistema Institucional de Gestión de Requerimientos,
--         Cambios y Auditoría Tecnológica
-- Versión: 1.0.0
-- Motor:  PostgreSQL 16+
-- Uso: psql -U sigrc_user -d sigrc -f 01_ddl.sql
-- ============================================================

-- Eliminar todo si existe (para reinicio limpio)
DROP SCHEMA IF EXISTS sigrc CASCADE;

-- ============================================================
-- ESQUEMA PRINCIPAL
-- ============================================================
CREATE SCHEMA sigrc AUTHORIZATION CURRENT_USER;
SET search_path TO sigrc, public;

-- ============================================================
-- 1. CATÁLOGOS BASE
-- ============================================================

CREATE TABLE roles (
    id_rol      SERIAL       PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permisos (
    id_permiso      SERIAL       PRIMARY KEY,
    codigo          VARCHAR(60)  NOT NULL UNIQUE,
    nombre          VARCHAR(150) NOT NULL,
    modulo          VARCHAR(50)  NOT NULL,
    tipo_acceso     VARCHAR(20)  NOT NULL CHECK (tipo_acceso IN ('CREAR','LEER','ACTUALIZAR','ELIMINAR','APROBAR','EXPORTAR','IMPORTAR')),
    descripcion     TEXT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles_permisos (
    id_rol_permiso SERIAL  PRIMARY KEY,
    id_rol         INTEGER NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE,
    id_permiso     INTEGER NOT NULL REFERENCES permisos(id_permiso) ON DELETE CASCADE,
    UNIQUE(id_rol, id_permiso)
);

CREATE TABLE areas (
    id_area         SERIAL       PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    responsable_id  INTEGER,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sistemas (
    id_sistema      SERIAL       PRIMARY KEY,
    codigo          VARCHAR(30)  NOT NULL UNIQUE,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    version_actual  VARCHAR(20),
    responsable_id  INTEGER,
    tecnologia      VARCHAR(100),
    estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','MANTENIMIENTO','OBSOLETO','EN_DESARROLLO')),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id_usuario      SERIAL       PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    cargo           VARCHAR(150),
    id_area         INTEGER      REFERENCES areas(id_area),
    id_rol          INTEGER      NOT NULL REFERENCES roles(id_rol),
    telefono        VARCHAR(20),
    debe_cambiar_password BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueado       BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos INTEGER    NOT NULL DEFAULT 0,
    ultimo_acceso   TIMESTAMP,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE areas ADD CONSTRAINT fk_area_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id_usuario);

ALTER TABLE sistemas ADD CONSTRAINT fk_sistema_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id_usuario);

CREATE TABLE categorias (
    id_categoria    SERIAL       PRIMARY KEY,
    codigo          VARCHAR(30)  NOT NULL UNIQUE,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subcategorias (
    id_subcategoria  SERIAL       PRIMARY KEY,
    id_categoria     INTEGER      NOT NULL REFERENCES categorias(id_categoria),
    codigo           VARCHAR(30)  NOT NULL UNIQUE,
    nombre           VARCHAR(150) NOT NULL,
    descripcion      TEXT,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE slas (
    id_sla          SERIAL       PRIMARY KEY,
    codigo          VARCHAR(30)  NOT NULL UNIQUE,
    nombre          VARCHAR(200) NOT NULL,
    tipo_ticket     VARCHAR(20)  NOT NULL CHECK (tipo_ticket IN ('INCIDENTE','REQUERIMIENTO','MEJORA','CAMBIO','CONSULTA','PROBLEMA')),
    id_categoria    INTEGER      REFERENCES categorias(id_categoria),
    prioridad       VARCHAR(15)  NOT NULL CHECK (prioridad IN ('CRITICA','ALTA','MEDIA','BAJA')),
    tiempo_respuesta_horas INTEGER NOT NULL,
    tiempo_solucion_horas  INTEGER NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. TABLAS OPERATIVAS
-- ============================================================

CREATE TABLE tickets (
    id_ticket           SERIAL          PRIMARY KEY,
    numero_ticket       VARCHAR(15)     NOT NULL UNIQUE,
    tipo                VARCHAR(20)     NOT NULL CHECK (tipo IN ('INCIDENTE','REQUERIMIENTO','MEJORA','CAMBIO','CONSULTA','PROBLEMA')),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'NUEVO' CHECK (estado IN (
                            'NUEVO','ASIGNADO','EN_ANALISIS','EN_DESARROLLO','EN_PRUEBAS',
                            'PENDIENTE_USUARIO','RESUELTO','CERRADO','RECHAZADO'
                        )),
    prioridad           VARCHAR(15)     NOT NULL CHECK (prioridad IN ('CRITICA','ALTA','MEDIA','BAJA')),
    id_solicitante      INTEGER         NOT NULL REFERENCES usuarios(id_usuario),
    id_area             INTEGER         NOT NULL REFERENCES areas(id_area),
    id_sistema          INTEGER         REFERENCES sistemas(id_sistema),
    id_categoria        INTEGER         REFERENCES categorias(id_categoria),
    id_subcategoria     INTEGER         REFERENCES subcategorias(id_subcategoria),
    id_responsable      INTEGER         REFERENCES usuarios(id_usuario),
    id_sla              INTEGER         REFERENCES slas(id_sla),
    asunto              VARCHAR(300)    NOT NULL,
    descripcion         TEXT            NOT NULL,
    impacto             VARCHAR(15)     CHECK (impacto IN ('EXTENSIVO','MODERADO','MENOR','LIMITADO')),
    urgencia            VARCHAR(15)     CHECK (urgencia IN ('INMEDIATA','ALTA','MEDIA','BAJA')),
    origen              VARCHAR(30)     DEFAULT 'SISTEMA' CHECK (origen IN ('SISTEMA','CORREO','TELEFONO','PRESENCIAL','REUNION','OTRO')),
    fecha_limite        TIMESTAMP,
    fecha_cierre        TIMESTAMP,
    causa_raiz          TEXT,
    solucion            TEXT,
    es_reabierto        BOOLEAN         NOT NULL DEFAULT FALSE,
    numero_reaperturas  INTEGER         NOT NULL DEFAULT 0,
    calificacion        INTEGER         CHECK (calificacion BETWEEN 1 AND 5),
    comentario_cierre   TEXT,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_estado       ON tickets(estado);
CREATE INDEX IF NOT EXISTS idx_tickets_prioridad    ON tickets(prioridad);
CREATE INDEX IF NOT EXISTS idx_tickets_solicitante  ON tickets(id_solicitante);
CREATE INDEX IF NOT EXISTS idx_tickets_responsable  ON tickets(id_responsable);
CREATE INDEX IF NOT EXISTS idx_tickets_area         ON tickets(id_area);
CREATE INDEX IF NOT EXISTS idx_tickets_sistema      ON tickets(id_sistema);
CREATE INDEX IF NOT EXISTS idx_tickets_creado_en    ON tickets(creado_en);
CREATE INDEX IF NOT EXISTS idx_tickets_tipo         ON tickets(tipo);

CREATE TABLE ticket_historial (
    id_historial    SERIAL       PRIMARY KEY,
    id_ticket       INTEGER      NOT NULL REFERENCES tickets(id_ticket) ON DELETE CASCADE,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20)  NOT NULL,
    id_usuario      INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    observacion     TEXT,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_historial_ticket ON ticket_historial(id_ticket);

CREATE TABLE ticket_comentarios (
    id_comentario   SERIAL       PRIMARY KEY,
    id_ticket       INTEGER      NOT NULL REFERENCES tickets(id_ticket) ON DELETE CASCADE,
    id_usuario      INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    comentario      TEXT         NOT NULL,
    es_interno      BOOLEAN      NOT NULL DEFAULT FALSE,
    editado         BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comentarios_ticket ON ticket_comentarios(id_ticket);

CREATE TABLE ticket_adjuntos (
    id_adjunto      SERIAL       PRIMARY KEY,
    id_ticket       INTEGER      NOT NULL REFERENCES tickets(id_ticket) ON DELETE CASCADE,
    id_comentario   INTEGER      REFERENCES ticket_comentarios(id_comentario) ON DELETE SET NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_archivo  VARCHAR(255) NOT NULL,
    ruta_archivo    VARCHAR(500) NOT NULL,
    tipo_mime       VARCHAR(100) NOT NULL,
    tamano_bytes     BIGINT       NOT NULL,
    hash_sha256     VARCHAR(64)  NOT NULL,
    id_usuario      INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_adjuntos_ticket ON ticket_adjuntos(id_ticket);

CREATE TABLE cambios (
    id_cambio           SERIAL       PRIMARY KEY,
    codigo_cambio       VARCHAR(15)  NOT NULL UNIQUE,
    id_ticket           INTEGER      REFERENCES tickets(id_ticket),
    id_sistema          INTEGER      REFERENCES sistemas(id_sistema),
    titulo              VARCHAR(300) NOT NULL,
    descripcion         TEXT         NOT NULL,
    justificacion       TEXT         NOT NULL,
    tipo                VARCHAR(30)  NOT NULL CHECK (tipo IN ('NORMAL','EMERGENCIA','ESTANDAR','MENOR')),
    impacto             VARCHAR(15)  NOT NULL CHECK (impacto IN ('ALTO','MEDIO','BAJO')),
    riesgo              VARCHAR(15)  NOT NULL CHECK (riesgo IN ('ALTO','MEDIO','BAJO')),
    estado              VARCHAR(20)  NOT NULL DEFAULT 'SOLICITADO' CHECK (estado IN (
                            'SOLICITADO','REVISADO','APROBADO','RECHAZADO','EN_IMPLEMENTACION',
                            'IMPLEMENTADO','VERIFICADO','CERRADO','CANCELADO'
                        )),
    id_solicitante      INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    id_aprobador        INTEGER      REFERENCES usuarios(id_usuario),
    id_responsable      INTEGER      REFERENCES usuarios(id_usuario),
    plan_implementacion TEXT,
    plan_retorno        TEXT,
    fecha_aprobacion    TIMESTAMP,
    fecha_inicio        TIMESTAMP,
    fecha_implementacion TIMESTAMP,
    fecha_verificacion  TIMESTAMP,
    resultado           VARCHAR(15)  CHECK (resultado IN ('EXITOSO','EXITOSO_CON_INCIDENTES','FALLIDO','CANCELADO')),
    lecciones_aprendidas TEXT,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cambios_estado  ON cambios(estado);
CREATE INDEX IF NOT EXISTS idx_cambios_ticket  ON cambios(id_ticket);
CREATE INDEX IF NOT EXISTS idx_cambios_sistema ON cambios(id_sistema);

CREATE TABLE versiones (
    id_version      SERIAL       PRIMARY KEY,
    id_sistema      INTEGER      NOT NULL REFERENCES sistemas(id_sistema),
    id_cambio       INTEGER      REFERENCES cambios(id_cambio),
    version         VARCHAR(20)  NOT NULL,
    tipo            VARCHAR(20)  NOT NULL CHECK (tipo IN ('MAJOR','MINOR','PATCH','HOTFIX','RELEASE')),
    descripcion     TEXT         NOT NULL,
    notas_liberacion TEXT,
    id_responsable  INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    fecha_despliegue TIMESTAMP,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','EN_DESPLIEGUE','DESPLEGADO','FALLIDO','RECHAZADO')),
    ambiente        VARCHAR(20)  NOT NULL DEFAULT 'PRODUCCION' CHECK (ambiente IN ('DESARROLLO','PRUEBAS','CERTIFICACION','PRODUCCION')),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id_sistema, version)
);

CREATE INDEX IF NOT EXISTS idx_versiones_sistema ON versiones(id_sistema);

CREATE TABLE versiones_tickets (
    id_version_ticket SERIAL  PRIMARY KEY,
    id_version        INTEGER NOT NULL REFERENCES versiones(id_version) ON DELETE CASCADE,
    id_ticket         INTEGER NOT NULL REFERENCES tickets(id_ticket) ON DELETE CASCADE,
    UNIQUE(id_version, id_ticket)
);

-- ============================================================
-- 3. BASE DE CONOCIMIENTO
-- ============================================================
CREATE TABLE base_conocimiento (
    id_articulo     SERIAL       PRIMARY KEY,
    titulo          VARCHAR(300) NOT NULL,
    contenido       TEXT         NOT NULL,
    tipo            VARCHAR(20)  NOT NULL CHECK (tipo IN ('SOLUCION','MANUAL','PROCEDIMIENTO','FAQ','GUIA')),
    id_categoria    INTEGER      REFERENCES categorias(id_categoria),
    id_sistema      INTEGER      REFERENCES sistemas(id_sistema),
    palabras_clave  TEXT,
    id_autor        INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    id_revisor      INTEGER      REFERENCES usuarios(id_usuario),
    version         INTEGER      NOT NULL DEFAULT 1,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','PUBLICADO','ARCHIVADO')),
    contenido_html  TEXT,
    adjuntos        JSONB,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bc_tipo     ON base_conocimiento(tipo);
CREATE INDEX IF NOT EXISTS idx_bc_sistema  ON base_conocimiento(id_sistema);

-- ============================================================
-- 4. AUDITORÍA GENERAL
-- ============================================================
CREATE TABLE auditoria (
    id_auditoria    SERIAL       PRIMARY KEY,
    id_usuario      INTEGER      REFERENCES usuarios(id_usuario),
    username        VARCHAR(50)  NOT NULL,
    accion          VARCHAR(50)  NOT NULL,
    tipo_operacion  VARCHAR(20)  NOT NULL CHECK (tipo_operacion IN ('CREATE','READ','UPDATE','DELETE','LOGIN','LOGOUT','EXPORT','APROBAR','RECHAZAR','REASIGNAR')),
    tabla_afectada  VARCHAR(100),
    id_registro     INTEGER,
    datos_anteriores JSONB,
    datos_nuevos     JSONB,
    direccion_ip    VARCHAR(45)  NOT NULL,
    user_agent      VARCHAR(500),
    sesion_id       VARCHAR(100),
    resultado       VARCHAR(10)  NOT NULL CHECK (resultado IN ('EXITO','FRACASO')),
    detalle         TEXT,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria(id_usuario);
CREATE INDEX IF NOT EXISTS idx_auditoria_accion  ON auditoria(accion);
CREATE INDEX IF NOT EXISTS idx_auditoria_tabla   ON auditoria(tabla_afectada);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha   ON auditoria(creado_en);

-- ============================================================
-- 5. NOTIFICACIONES
-- ============================================================
CREATE TABLE notificaciones (
    id_notificacion SERIAL       PRIMARY KEY,
    id_destinatario INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    id_ticket       INTEGER      REFERENCES tickets(id_ticket),
    tipo            VARCHAR(30)  NOT NULL CHECK (tipo IN (
                        'TICKET_CREADO','TICKET_ASIGNADO','TICKET_RESUELTO','TICKET_CERRADO',
                        'TICKET_REABIERTO','COMENTARIO_NUEVO','CAMBIO_ESTADO','VENCIMIENTO_PROXIMO',
                        'SLA_ALERTA','CAMBIO_APROBADO','VERSION_DESPLEGADA','MENSAJE_INTERNO'
                    )),
    asunto          VARCHAR(300) NOT NULL,
    mensaje         TEXT         NOT NULL,
    leido           BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_lectura   TIMESTAMP,
    enviado_correo  BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_destinatario ON notificaciones(id_destinatario);
CREATE INDEX IF NOT EXISTS idx_notif_leido        ON notificaciones(leido);

-- ============================================================
-- 6. INDICADORES DE GESTIÓN
-- ============================================================
CREATE TABLE indicadores_cache (
    id_indicador    SERIAL       PRIMARY KEY,
    tipo            VARCHAR(50)  NOT NULL,
    periodo         VARCHAR(7)   NOT NULL,
    valor           NUMERIC(15,2) NOT NULL,
    meta            NUMERIC(15,2),
    cumplimiento    NUMERIC(5,2),
    dimension       VARCHAR(50),
    dimension_id    INTEGER,
    metadatos       JSONB,
    calculado_en    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tipo, periodo, dimension, COALESCE(dimension_id, 0))
);

-- ============================================================
-- 7. CONFIGURACIÓN DEL SISTEMA
-- ============================================================
CREATE TABLE configuracion (
    id_config       SERIAL       PRIMARY KEY,
    grupo           VARCHAR(50)  NOT NULL,
    clave           VARCHAR(100) NOT NULL,
    valor           TEXT         NOT NULL,
    tipo_dato       VARCHAR(20)  NOT NULL DEFAULT 'TEXTO' CHECK (tipo_dato IN ('TEXTO','NUMERO','BOOLEANO','JSON','CORREO')),
    descripcion     TEXT,
    UNIQUE(grupo, clave)
);

-- ============================================================
-- 8. SESIONES DE USUARIO
-- ============================================================
CREATE TABLE sesiones (
    id_sesion       SERIAL       PRIMARY KEY,
    id_usuario      INTEGER      NOT NULL REFERENCES usuarios(id_usuario),
    token           VARCHAR(500) NOT NULL UNIQUE,
    refresh_token   VARCHAR(500),
    direccion_ip    VARCHAR(45),
    user_agent      VARCHAR(500),
    fecha_inicio    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP   NOT NULL,
    fecha_cierre    TIMESTAMP,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_sesiones_usuario ON sesiones(id_usuario);
CREATE INDEX IF NOT EXISTS idx_sesiones_token   ON sesiones(token);

-- ============================================================
-- FUNCIONES Y TRIGGERS
-- ============================================================

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

DROP TRIGGER IF EXISTS trg_generar_numero_ticket ON tickets;
CREATE TRIGGER trg_generar_numero_ticket
    BEFORE INSERT ON tickets
    FOR EACH ROW
    WHEN (NEW.numero_ticket IS NULL)
    EXECUTE FUNCTION generar_numero_ticket();

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

DROP TRIGGER IF EXISTS trg_generar_codigo_cambio ON cambios;
CREATE TRIGGER trg_generar_codigo_cambio
    BEFORE INSERT ON cambios
    FOR EACH ROW
    WHEN (NEW.codigo_cambio IS NULL)
    EXECUTE FUNCTION generar_codigo_cambio();

CREATE OR REPLACE FUNCTION actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tickets_updated ON tickets;
CREATE TRIGGER trg_tickets_updated
    BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

DROP TRIGGER IF EXISTS trg_cambios_updated ON cambios;
CREATE TRIGGER trg_cambios_updated
    BEFORE UPDATE ON cambios
    FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE OR REPLACE FUNCTION auditar_tickets()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO auditoria (
        id_usuario, username, accion, tipo_operacion,
        tabla_afectada, id_registro, datos_anteriores, datos_nuevos,
        direccion_ip, user_agent, resultado
    ) VALUES (
        COALESCE(NEW.id_responsable, NEW.id_solicitante),
        (SELECT username FROM usuarios WHERE id_usuario = COALESCE(NEW.id_responsable, NEW.id_solicitante)),
        CASE
            WHEN TG_OP = 'INSERT' THEN 'CREACION DE TICKET'
            WHEN TG_OP = 'UPDATE' THEN 'ACTUALIZACION DE TICKET'
            WHEN TG_OP = 'DELETE' THEN 'ELIMINACION DE TICKET'
        END,
        CASE TG_OP
            WHEN 'INSERT' THEN 'CREATE'
            WHEN 'UPDATE' THEN 'UPDATE'
            WHEN 'DELETE' THEN 'DELETE'
        END,
        'tickets',
        COALESCE(NEW.id_ticket, OLD.id_ticket),
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN
            row_to_json(OLD)::JSONB
        ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN
            row_to_json(NEW)::JSONB
        ELSE NULL END,
        '0.0.0.0',
        'SISTEMA',
        'EXITO'
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_tickets ON tickets;
CREATE TRIGGER trg_auditoria_tickets
    AFTER INSERT OR UPDATE OR DELETE ON tickets
    FOR EACH ROW EXECUTE FUNCTION auditar_tickets();

-- ============================================================
-- VISTAS
-- ============================================================

CREATE OR REPLACE VIEW v_tickets_activos AS
SELECT
    t.id_ticket,
    t.numero_ticket,
    t.tipo,
    t.estado,
    t.prioridad,
    t.asunto,
    t.descripcion,
    t.fecha_limite,
    t.fecha_cierre,
    t.creado_en,
    t.actualizado_en,
    sol.username AS solicitante_username,
    sol.nombres || ' ' || sol.apellidos AS nombre_solicitante,
    a.nombre AS area,
    s.nombre AS sistema,
    cat.nombre AS categoria,
    sub.nombre AS subcategoria,
    res.username AS responsable_username,
    res.nombres || ' ' || res.apellidos AS nombre_responsable,
    sla.nombre AS sla,
    EXTRACT(EPOCH FROM (COALESCE(t.fecha_cierre, CURRENT_TIMESTAMP) - t.creado_en))/3600 AS horas_transcurridas
FROM tickets t
JOIN usuarios sol      ON t.id_solicitante = sol.id_usuario
JOIN areas a           ON t.id_area = a.id_area
LEFT JOIN sistemas s   ON t.id_sistema = s.id_sistema
LEFT JOIN categorias cat  ON t.id_categoria = cat.id_categoria
LEFT JOIN subcategorias sub ON t.id_subcategoria = sub.id_subcategoria
LEFT JOIN usuarios res ON t.id_responsable = res.id_usuario
LEFT JOIN slas sla     ON t.id_sla = sla.id_sla
WHERE t.activo = TRUE
  AND t.estado NOT IN ('CERRADO','RECHAZADO');

CREATE OR REPLACE VIEW v_cumplimiento_sla AS
SELECT
    t.tipo,
    t.prioridad,
    COUNT(*) AS total_tickets,
    SUM(CASE WHEN t.fecha_cierre IS NOT NULL
             AND t.fecha_cierre <= t.fecha_limite THEN 1 ELSE 0 END) AS dentro_sla,
    ROUND(
        100.0 * SUM(CASE WHEN t.fecha_cierre IS NOT NULL
                    AND t.fecha_cierre <= t.fecha_limite THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0), 2
    ) AS porcentaje_cumplimiento
FROM tickets t
WHERE t.estado IN ('CERRADO','RESUELTO')
GROUP BY t.tipo, t.prioridad;

-- ============================================================
-- DATOS INICIALES
-- ============================================================

INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('ADMIN',       'Administrador',        'Acceso total al sistema'),
    ('JEFE_TI',     'Jefe de TI',           'Supervision y aprobacion de cambios'),
    ('TECNICO',     'Tecnico de Soporte',   'Atencion y resolucion de tickets'),
    ('AUDITOR',     'Auditor',              'Consulta de auditoria y reportes'),
    ('SUPERVISOR',  'Supervisor',           'Supervision de tickets y reportes'),
    ('SOLICITANTE', 'Usuario Solicitante',  'Creacion y seguimiento de tickets');

INSERT INTO permisos (codigo, nombre, modulo, tipo_acceso) VALUES
    ('TICKET_CREAR',      'Crear Tickets',             'TICKETS',  'CREAR'),
    ('TICKET_LEER',       'Leer Tickets',              'TICKETS',  'LEER'),
    ('TICKET_ACTUALIZAR', 'Actualizar Tickets',        'TICKETS',  'ACTUALIZAR'),
    ('TICKET_ELIMINAR',   'Eliminar Tickets',           'TICKETS',  'ELIMINAR'),
    ('TICKET_ASIGNAR',    'Asignar Tickets',            'TICKETS',  'ACTUALIZAR'),
    ('CAMBIO_CREAR',      'Crear Cambios',             'CAMBIOS',  'CREAR'),
    ('CAMBIO_APROBAR',    'Aprobar Cambios',           'CAMBIOS',  'APROBAR'),
    ('USUARIO_ADMIN',     'Administrar Usuarios',      'USUARIOS', 'CREAR'),
    ('AUDITORIA_LEER',    'Consultar Auditoria',       'AUDITORIA','LEER'),
    ('REPORTE_EXPORTAR',  'Exportar Reportes',         'REPORTES', 'EXPORTAR'),
    ('DASHBOARD_LEER',    'Ver Dashboard',             'DASHBOARD','LEER'),
    ('CONFIGURAR',        'Configurar Sistema',        'CONFIG',   'ACTUALIZAR');

INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.codigo = 'ADMIN';

INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.codigo = 'JEFE_TI'
  AND p.codigo IN ('TICKET_CREAR','TICKET_LEER','TICKET_ACTUALIZAR','TICKET_ASIGNAR',
                   'CAMBIO_CREAR','CAMBIO_APROBAR','DASHBOARD_LEER','REPORTE_EXPORTAR');

INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.codigo = 'TECNICO'
  AND p.codigo IN ('TICKET_CREAR','TICKET_LEER','TICKET_ACTUALIZAR','DASHBOARD_LEER');

INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.codigo = 'AUDITOR'
  AND p.codigo IN ('AUDITORIA_LEER','REPORTE_EXPORTAR','DASHBOARD_LEER');

INSERT INTO roles_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.codigo = 'SOLICITANTE'
  AND p.codigo IN ('TICKET_CREAR','TICKET_LEER');

INSERT INTO areas (codigo, nombre) VALUES
    ('SISTEMAS',     'Sistemas Informaticos'),
    ('GERENCIA',     'Gerencia General'),
    ('ADMIN',        'Administracion'),
    ('FINANZAS',     'Direccion Financiera'),
    ('OPERACIONES',  'Operaciones'),
    ('COMERCIAL',    'Comercializacion'),
    ('TECNICA',      'Direccion Tecnica'),
    ('RECURSOS_H',   'Recursos Humanos'),
    ('ASESORIA_J',   'Asesoria Juridica'),
    ('PLANIFICACION','Planificacion y Presupuesto');

INSERT INTO categorias (codigo, nombre) VALUES
    ('HARDWARE',     'Hardware'),
    ('SOFTWARE',     'Software'),
    ('RED',          'Red y Conectividad'),
    ('SEGURIDAD',    'Seguridad Informatica'),
    ('BD',           'Base de Datos'),
    ('CORREO',       'Correo Electronico'),
    ('SISTEMA_ADM',  'Sistema Administrativo'),
    ('SISTEMA_COM',  'Sistema Comercial'),
    ('SISTEMA_TEC',  'Sistema Tecnico'),
    ('OTROS',        'Otros');

INSERT INTO subcategorias (id_categoria, codigo, nombre) VALUES
    (1, 'PC',           'Computadora de Escritorio'),
    (1, 'LAPTOP',       'Laptop'),
    (1, 'IMPRESORA',    'Impresora'),
    (1, 'PERIFERICO',   'Periferico'),
    (2, 'SISTEMA_OPE',  'Sistema Operativo'),
    (2, 'OFIMATICA',    'Ofimatica'),
    (2, 'ERP',          'ERP/Sistema de Gestion'),
    (2, 'ANTIVIRUS',    'Antivirus'),
    (3, 'WIFI',         'Red Inalambrica'),
    (3, 'LAN',          'Red Cableada'),
    (3, 'VPN',          'VPN'),
    (3, 'INTERNET',     'Internet'),
    (4, 'ACCESO',       'Control de Acceso'),
    (4, 'USUARIO',      'Gestion de Usuarios'),
    (4, 'AUDITORIA_SG', 'Auditoria de Seguridad'),
    (5, 'CONSULTA',     'Consulta SQL'),
    (5, 'MIGRACION',    'Migracion de Datos'),
    (5, 'RESPALDO',     'Respaldo y Restauracion');

INSERT INTO slas (codigo, nombre, tipo_ticket, prioridad, tiempo_respuesta_horas, tiempo_solucion_horas) VALUES
    ('SLACRI',   'SLA Critico',       'INCIDENTE',   'CRITICA',  1,  4),
    ('SLAALTA',  'SLA Alta',          'INCIDENTE',   'ALTA',     2,  8),
    ('SLAMED',   'SLA Media',         'INCIDENTE',   'MEDIA',    4,  24),
    ('SLABAJ',   'SLA Baja',          'INCIDENTE',   'BAJA',     8,  48),
    ('SLAREQAL', 'SLA Requerimiento Alto',  'REQUERIMIENTO', 'ALTA',   4,  40),
    ('SLAREQME', 'SLA Requerimiento Medio', 'REQUERIMIENTO', 'MEDIA',  8,  80);
