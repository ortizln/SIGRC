package com.epmapa.sigrc.domain.dto;

public record ResponsableAsignadoDTO(
    Integer idUsuario,
    String nombre,
    String sumilla,
    String puestoFirmante,
    String unidadFirmante,
    Integer idDelegacion,
    Integer usuarioOriginal,
    String usuarioOriginalNombre,
    Boolean delegacionAplicada
) {}
