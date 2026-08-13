package com.epmapa.sigrc.domain.dto;

public record UnidadOrganizacionalDTO(
    Integer idUnidad,
    String codigo,
    String nombre,
    String sigla,
    String descripcion,
    String tipoUnidad,
    Integer idNivel,
    String nivelNombre,
    Integer idUnidadPadre,
    String unidadPadreNombre,
    Integer responsableAsignacionId,
    Integer orden,
    Boolean activo
) {}
