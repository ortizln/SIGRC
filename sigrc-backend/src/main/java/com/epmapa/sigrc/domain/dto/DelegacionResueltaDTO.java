package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record DelegacionResueltaDTO(
    Integer idUsuarioDelegado,
    Integer idDelegacion,
    Integer idUsuarioOriginal,
    String nombreDelegado,
    String nombreOriginal,
    String tipoDelegacion,
    LocalDate fechaInicio,
    LocalDate fechaFin
) {}
