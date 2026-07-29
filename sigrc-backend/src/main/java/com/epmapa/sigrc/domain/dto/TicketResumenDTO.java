package com.epmapa.sigrc.domain.dto;

public record TicketResumenDTO(
    Integer id,
    String numeroTicket,
    String asunto,
    String estado,
    String prioridad,
    String tipo,
    String creadoEn
) {}
