

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
