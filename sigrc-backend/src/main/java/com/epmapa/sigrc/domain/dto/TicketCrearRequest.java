package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketCrearRequest(
    @NotBlank String tipo,
    @NotBlank String prioridad,
    @NotNull Integer idSolicitante,
    @NotNull Integer idArea,
    Integer idSistema,
    Integer idCategoria,
    Integer idSubcategoria,
    @NotBlank String asunto,
    @NotBlank String descripcion,
    String impacto,
    String urgencia,
    String origen
) {}
