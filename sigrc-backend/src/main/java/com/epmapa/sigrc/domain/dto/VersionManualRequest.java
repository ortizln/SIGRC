package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record VersionManualRequest(
    String nombre,
    String version,
    LocalDate fechaAprobacion,
    LocalDate fechaVigencia,
    Long documentoId,
    String observaciones
) {}