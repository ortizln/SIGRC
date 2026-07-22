package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record AppMovilDTO(
    Integer idAppMovil,
    String version,
    String nombreArchivo,
    String rutaArchivo,
    Long tamanioBytes,
    String descripcion,
    String checksum,
    Boolean activo,
    LocalDateTime creadoEn
) {}
