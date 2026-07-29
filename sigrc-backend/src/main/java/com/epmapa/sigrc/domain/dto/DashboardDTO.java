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

    long totalDocumentos,
    long pendientesRespuesta,
    long documentosVencidos,
    long documentosQueGeneraronTicket,
    double tiempoPromedioRespuestaHoras,

    long cambiosSolicitados,
    long cambiosAprobados,
    long cambiosCompletados,

    String versionActual,
    String sistemaReciente,
    String ultimoCambioDescripcion,
    String fechaUltimoCambio,

    List<Map<String, Object>> memosPendientes,

    List<Map<String, Object>> ticketsPorEstado,
    List<Map<String, Object>> ticketsPorPrioridad,
    List<Map<String, Object>> ticketsPorArea,
    List<Map<String, Object>> ticketsPorSistema,
    List<Map<String, Object>> ticketsPorTipo,

    List<Map<String, Object>> documentosPorEstado,
    List<Map<String, Object>> documentosPorPrioridad,

    List<Map<String, Object>> cambiosPorEstado,
    List<Map<String, Object>> cambiosPorImpacto,

    List<Map<String, Object>> tendenciasMensuales
) {}
