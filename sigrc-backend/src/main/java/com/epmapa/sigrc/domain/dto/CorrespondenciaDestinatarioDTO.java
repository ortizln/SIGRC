package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record CorrespondenciaDestinatarioDTO(
    Integer idCorrespondenciaDestinatario,
    String tipo,
    Integer idDestinatario,
    String nombre,
    Boolean recibido,
    LocalDateTime fechaRecibido
) {}