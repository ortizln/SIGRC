package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record VersionManualDTO(
    Integer idVersionManual,
    String nombre,
    String version,
    LocalDate fechaAprobacion,
    LocalDate fechaVigencia,
    Long documentoId,
    String estado,
    String observaciones
) {}