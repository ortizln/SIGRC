-- ============================================================
-- MIGRACIÓN: Bandeja Compartida Controlada (BCC)
-- SIGRC v1.1.0 — Ampliación funcional
-- Fecha: 2026-10-19
-- Descripción: Vínculo administrativo asistente→titular y
--              lectura personal por usuario.
-- Idempotente: seguro de re-ejecutar.
-- ============================================================

BEGIN;

-- -----------------------------------------------------------
-- 1. Vínculo administrativo asistente → titular
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigrc.correspondencia_vinculo_asistente (
    id_vinculo           BIGSERIAL PRIMARY KEY,
    puesto_asistente_id  INTEGER NOT NULL REFERENCES sigrc.puesto(id_puesto),
    puesto_titular_id    INTEGER NOT NULL REFERENCES sigrc.puesto(id_puesto),
    nombre_asistente     VARCHAR(200),
    nombre_titular       VARCHAR(200),
    puede_recepcionar    BOOLEAN NOT NULL DEFAULT TRUE,
    puede_organizar      BOOLEAN NOT NULL DEFAULT TRUE,
    puede_seguimiento    BOOLEAN NOT NULL DEFAULT TRUE,
    vigente_desde        DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta        DATE,
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    autorizado_por       INTEGER NOT NULL REFERENCES sigrc.usuarios(id_usuario),
    creado_en            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en       TIMESTAMP,

    CONSTRAINT chk_vcc_asistente_distinto_titular
        CHECK (puesto_asistente_id <> puesto_titular_id),
    CONSTRAINT chk_vcc_fechas
        CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);

CREATE INDEX IF NOT EXISTS idx_vcc_asistente_activo
    ON sigrc.correspondencia_vinculo_asistente(puesto_asistente_id, activo);
CREATE INDEX IF NOT EXISTS idx_vcc_titular_activo
    ON sigrc.correspondencia_vinculo_asistente(puesto_titular_id, activo);

-- -----------------------------------------------------------
-- 2. Lectura personal por usuario (no global)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigrc.correspondencia_lectura_usuario (
    id_lectura           BIGSERIAL PRIMARY KEY,
    correspondencia_id   INTEGER NOT NULL REFERENCES sigrc.correspondencia(id_correspondencia),
    usuario_id           INTEGER NOT NULL REFERENCES sigrc.usuarios(id_usuario),
    leido_en             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_lectura_usuario_doc
        UNIQUE (correspondencia_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_lectura_usuario
    ON sigrc.correspondencia_lectura_usuario(usuario_id, correspondencia_id);

-- -----------------------------------------------------------
-- 3. Idempotencia de recepción delegada
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigrc.correspondencia_recepcion_idempotente (
    id_clave             BIGSERIAL PRIMARY KEY,
    idempotency_key      VARCHAR(128) NOT NULL UNIQUE,
    correspondencia_id   INTEGER NOT NULL REFERENCES sigrc.correspondencia(id_correspondencia),
    usuario_id           INTEGER NOT NULL REFERENCES sigrc.usuarios(id_usuario),
    puesto_titular_id    INTEGER NOT NULL,
    creado_en            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recep_idem_usuario
    ON sigrc.correspondencia_recepcion_idempotente(usuario_id);

COMMIT;
