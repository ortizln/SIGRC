package com.epmapa.sigrc.domain.dto;

public record NivelOrganizacionalDTO(
    Integer idNivel,
    String codigo,
    String nombre,
    String descripcion,
    Integer orden,
    Boolean activo
) {}
