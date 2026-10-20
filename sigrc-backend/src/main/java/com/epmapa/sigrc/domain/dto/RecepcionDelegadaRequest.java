package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecepcionDelegadaRequest(
    @NotNull Integer correspondenciaId,
    @NotNull Integer puestoTitularId,
    @NotBlank String idempotencyKey,
    String observacion
) {}
