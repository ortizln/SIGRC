package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record PermisoDTO(
    Integer idPermiso,
    String codigo,
    String nombre,
    String modulo,
    String tipoAcceso,
    String descripcion,
    Boolean activo,
    LocalDateTime creadoEn
) {}
