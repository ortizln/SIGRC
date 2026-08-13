package com.epmapa.sigrc.domain.dto;

public record UnidadOrganizacionalRequest(
    String codigo,
    String nombre,
    String sigla,
    String descripcion,
    String tipoUnidad,
    Integer idNivel,
    Integer idUnidadPadre,
    Integer orden
) {}
