

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
