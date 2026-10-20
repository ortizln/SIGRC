package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecepcionDelegadaRequest(
    @NotNull Integer correspondenciaId,
    /**
     * Puesto del titular. Si es {@code null}, el servidor lo resuelve
     * a partir de los titulares vinculados que participan en el documento.
     */
    Integer puestoTitularId,
    @NotBlank String idempotencyKey,
    String observacion
) {}
