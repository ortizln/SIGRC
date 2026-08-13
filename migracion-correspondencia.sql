

-- Migraciones incrementales de correspondencia.
-- Idempotente: puede ejecutarse varias veces sin duplicar datos.
SET search_path TO sigrc, public;

-- 2. Agregar columna sentido a correspondencia
ALTER TABLE correspondencia
  ADD COLUMN IF NOT EXISTS sentido VARCHAR(10);

UPDATE correspondencia SET sentido = 'INGRESO' WHERE sentido IS NULL;

ALTER TABLE correspondencia
  ALTER COLUMN sentido SET NOT NULL;

-- 3. Crear tabla de referencias entre documentos (ManyToMany)
CREATE TABLE IF NOT EXISTS correspondencia_referencia (
  id_correspondencia INTEGER NOT NULL REFERENCES correspondencia(id_correspondencia),
  id_referencia      INTEGER NOT NULL REFERENCES correspondencia(id_correspondencia),
  PRIMARY KEY (id_correspondencia, id_referencia)
);

-- 4. Tabla de destinatarios para documentos de SALIDA (relacional)
CREATE TABLE IF NOT EXISTS correspondencia_destinatario (
  id_correspondencia_destinatario BIGSERIAL PRIMARY KEY,
  id_correspondencia              BIGINT       NOT NULL,
  tipo                            VARCHAR(20)  NOT NULL,
  id_destinatario                 BIGINT       NOT NULL,
  nombre                          VARCHAR(300) NOT NULL,
  FOREIGN KEY (id_correspondencia) REFERENCES correspondencia(id_correspondencia) ON DELETE CASCADE
);

COMMENT ON COLUMN correspondencia_destinatario.tipo IS 'USUARIO o AREA';

-- 5. Tabla de responsables múltiples (ManyToMany)
CREATE TABLE IF NOT EXISTS sigrc.correspondencia_responsable (
  id_correspondencia INTEGER NOT NULL REFERENCES sigrc.correspondencia(id_correspondencia),
  id_usuario         INTEGER NOT NULL REFERENCES sigrc.usuarios(id_usuario),
  PRIMARY KEY (id_correspondencia, id_usuario)
);

-- Migrar responsables existentes desde columna id_responsable (solo si la columna existe)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_schema = 'sigrc' AND table_name = 'correspondencia' AND column_name = 'id_responsable') THEN
    INSERT INTO sigrc.correspondencia_responsable (id_correspondencia, id_usuario, sumilla, creado_en)
    SELECT c.id_correspondencia, c.id_responsable, '', NOW()
    FROM sigrc.correspondencia c
    WHERE c.id_responsable IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM sigrc.correspondencia_responsable cr
        WHERE cr.id_correspondencia = c.id_correspondencia AND cr.id_usuario = c.id_responsable
      );

    ALTER TABLE sigrc.correspondencia DROP COLUMN IF EXISTS id_responsable;
  END IF;
END $$;
