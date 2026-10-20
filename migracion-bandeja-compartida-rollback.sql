-- ============================================================
-- REVERSA DE MIGRACIÓN: Bandeja Compartida Controlada (BCC)
-- SIGRC v1.1.0
-- Fecha: 2026-10-19
-- Descripción: Elimina las tablas creadas por
--              migracion-bandeja-compartida.sql
--
-- ADVERTENCIA: destruye datos de recepción delegada,
--              lecturas personales e idempotencia.
-- Ejecutar solo con respaldo previo (pg_dump) y
-- confirmación del responsable del sistema.
-- ============================================================

BEGIN;

DROP TABLE IF EXISTS sigrc.correspondencia_recepcion_idempotente CASCADE;
DROP TABLE IF EXISTS sigrc.correspondencia_lectura_usuario CASCADE;
DROP TABLE IF EXISTS sigrc.correspondencia_vinculo_asistente CASCADE;

COMMIT;

-- Verificación post-reversa (debe retornar 0 en las tres):
-- SELECT COUNT(*) FROM information_schema.tables
--  WHERE table_schema = 'sigrc'
--    AND table_name IN ('correspondencia_recepcion_idempotente',
--                       'correspondencia_lectura_usuario',
--                       'correspondencia_vinculo_asistente');
