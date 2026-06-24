package com.epmapa.sigrc.domain.dto;

public record TicketVinculadoDTO(
    Integer idTicket,
    String numeroTicket,
    String asunto,
    String estado
) {}
