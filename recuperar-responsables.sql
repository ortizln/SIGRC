-- Recuperar asignaciones de responsables desde el historial
-- Los registros de ASIGNACION tienen detalle = 'Asignado a: Nombre Apellido'

INSERT INTO sigrc.correspondencia_responsable (id_correspondencia, id_usuario)
SELECT DISTINCT h.id_correspondencia, u.id_usuario
FROM sigrc.correspondencia_historial h
JOIN sigrc.usuarios u ON h.detalle LIKE '%' || u.nombres || '%'
WHERE h.accion = 'ASIGNACION'
  AND h.detalle LIKE 'Asignado a:%'
  AND NOT EXISTS (
    SELECT 1 FROM sigrc.correspondencia_responsable cr
    WHERE cr.id_correspondencia = h.id_correspondencia
      AND cr.id_usuario = u.id_usuario
  )
ON CONFLICT DO NOTHING;
