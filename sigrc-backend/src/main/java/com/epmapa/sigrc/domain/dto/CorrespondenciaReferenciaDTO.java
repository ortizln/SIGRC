package com.epmapa.sigrc.domain.dto;

public record CorrespondenciaReferenciaDTO(
    Integer idCorrespondencia,
    String numeroInterno,
    String asunto,
    String codigoDocumento
) {}