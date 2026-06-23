package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RolDTO(
    Integer idRol,
    String codigo,
    String nombre,
    String descripcion,
    Boolean activo,
    LocalDateTime creadoEn,
    List<Integer> permisoIds
) {}
