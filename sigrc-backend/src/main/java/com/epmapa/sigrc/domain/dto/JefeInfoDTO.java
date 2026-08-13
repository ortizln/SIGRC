package com.epmapa.sigrc.domain.dto;

public record JefeInfoDTO(
    Integer idJefe,
    String nombreJefe,
    Integer idPuesto,
    String puestoNombre,
    Integer idUnidad,
    String unidadNombre,
    String tipoAsignacion,
    String fechaInicio
) {}
