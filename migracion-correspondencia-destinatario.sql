-- Tabla de destinatarios para documentos de SALIDA
CREATE TABLE sigrc.correspondencia_destinatario (
    id_correspondencia_destinatario BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_correspondencia BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL COMMENT 'USUARIO o AREA',
    id_destinatario BIGINT NOT NULL,
    nombre VARCHAR(300) NOT NULL,
    FOREIGN KEY (id_correspondencia) REFERENCES sigrc.correspondencia(id_correspondencia) ON DELETE CASCADE
);
