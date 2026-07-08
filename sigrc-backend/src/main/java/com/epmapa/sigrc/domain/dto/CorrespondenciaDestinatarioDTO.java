package com.epmapa.sigrc.domain.dto;

public record CorrespondenciaDestinatarioDTO(
    Integer idCorrespondenciaDestinatario,
    String tipo,
    Integer idDestinatario,
    String nombre
) {}
