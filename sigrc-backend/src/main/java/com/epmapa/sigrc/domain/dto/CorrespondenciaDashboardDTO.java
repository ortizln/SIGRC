package com.epmapa.sigrc.domain.dto;

import java.util.List;
import java.util.Map;

public record CorrespondenciaDashboardDTO(
    long totalDocumentos,
    long pendientesRespuesta,
    long respondidos,
    long vencidos,
    long queGeneraronTicket,
    double tiempoPromedioRespuestaHoras,
    List<ItemCount> porEstado,
    List<ItemCount> porPrioridad,
    List<ItemCount> porTipoDocumento,
    List<ItemCount> porDepartamentoRemitente,
    List<TendenciaMensual> tendenciasMensuales
) {
    public record ItemCount(String label, long cantidad) {}
    public record TendenciaMensual(String mes, long cantidad) {}
}
