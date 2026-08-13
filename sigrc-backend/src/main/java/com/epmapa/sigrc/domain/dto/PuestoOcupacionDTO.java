package com.epmapa.sigrc.domain.dto;

public record PuestoOcupacionDTO(
    Integer idPuesto,
    String codigo,
    String nombre,
    Boolean esJefatura,
    Boolean esResponsableUnidad,
    Integer numeroPlazas,
    Integer ocupados,
    Integer vacantes
) {}
