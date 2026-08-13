package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record CorrespondenciaReferenciaDTO(
    Integer idCorrespondencia,
    String numeroInterno,
    String asunto,
    String codigoDocumento,
    LocalDate fechaDocumento
) {}