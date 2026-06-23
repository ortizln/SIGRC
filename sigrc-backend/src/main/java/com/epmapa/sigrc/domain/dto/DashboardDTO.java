package com.epmapa.sigrc.domain.dto;

import java.util.List;
import java.util.Map;

public record DashboardDTO(
    long ticketsAbiertos,
    long ticketsCerrados,
    long ticketsVencidos,
    long ticketsSinAsignar,
    double tiempoPromedioAtencionHoras,
    double cumplimientoSLA,
    List<Map<String, Object>> ticketsPorEstado,
    List<Map<String, Object>> ticketsPorPrioridad,
    List<Map<String, Object>> ticketsPorArea,
    List<Map<String, Object>> ticketsPorSistema,
    List<Map<String, Object>> ticketsPorTipo,
    List<Map<String, Object>> tendenciasMensuales
) {}
