package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CorrespondenciaRespuestaRequest(
    @NotNull Integer idCorrespondencia,
    @NotNull LocalDate fechaRespuesta,
    String numeroDocumento,
    Integer idTipoDocumento,
    Integer idResponsable,
    String observaciones
) {}
