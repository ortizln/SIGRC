-- Arreglar FK de correspondencia_responsable si apunta a tabla incorrecta
DO $$
BEGIN
  -- Eliminar FK existente (si fue creada con REFERENCES public.usuario)
  IF EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
    WHERE tc.table_schema = 'sigrc'
      AND tc.table_name = 'correspondencia_responsable'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND ccu.table_schema = 'public'
  ) THEN
    ALTER TABLE sigrc.correspondencia_responsable
      DROP CONSTRAINT correspondencia_responsable_id_usuario_fkey;
  END IF;

  -- Si no existe FK, recrearla apuntando a sigrc.usuarios
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
    WHERE tc.table_schema = 'sigrc'
      AND tc.table_name = 'correspondencia_responsable'
      AND tc.constraint_type = 'FOREIGN KEY'
  ) THEN
    ALTER TABLE sigrc.correspondencia_responsable
      ADD CONSTRAINT correspondencia_responsable_id_usuario_fkey
      FOREIGN KEY (id_usuario) REFERENCES sigrc.usuarios(id_usuario);
  END IF;
END $$;
