-- ============================================================
-- Migración: Convertir tabla join @ManyToMany a entidad @OneToMany
-- con campo sumilla para cada responsable asignado.
-- ============================================================

BEGIN;

-- 1. Renombrar la tabla join antigua (creada por @ManyToMany)
ALTER TABLE sigrc.correspondencia_responsable RENAME TO correspondencia_responsable_old;

-- 2. Crear la nueva tabla con PK propia y campo sumilla
CREATE TABLE sigrc.correspondencia_responsable (
    id SERIAL PRIMARY KEY,
    id_correspondencia INTEGER NOT NULL REFERENCES sigrc.correspondencia(id_correspondencia) ON DELETE CASCADE,
    id_usuario INTEGER NOT NULL REFERENCES sigrc.usuarios(id_usuario),
    sumilla TEXT DEFAULT '',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Migrar datos existentes (si los hay)
INSERT INTO sigrc.correspondencia_responsable (id_correspondencia, id_usuario, sumilla, creado_en)
SELECT cr.id_correspondencia, cr.id_usuario, '', NOW()
FROM sigrc.correspondencia_responsable_old cr;

-- 4. Eliminar tabla antigua
DROP TABLE sigrc.correspondencia_responsable_old;

COMMIT;
