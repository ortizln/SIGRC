package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.DashboardDTO;
import com.epmapa.sigrc.domain.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;

    public DashboardService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public DashboardDTO obtenerDashboard() {
        long abiertos = ticketRepository.contarAbiertos(List.of("CERRADO", "RECHAZADO"));
        long cerrados = ticketRepository.contarCerrados(List.of("CERRADO", "RECHAZADO"));
        long vencidos = ticketRepository.countVencidos();
        long sinAsignar = ticketRepository.countByEstado("NUEVO");
        double tiempoPromedio = Optional.ofNullable(ticketRepository.avgTiempoAtencionHoras()).orElse(0.0);

        return new DashboardDTO(
            abiertos, cerrados, vencidos, sinAsignar, tiempoPromedio,
            calcularCumplimientoSLA(),
            toMapList(ticketRepository.countByEstadoGroup(), "estado", "cantidad"),
            toMapList(ticketRepository.countByPrioridadGroup(), "prioridad", "cantidad"),
            toMapList(ticketRepository.countByAreaGroup(), "area", "cantidad"),
            toMapList(ticketRepository.countBySistemaGroup(), "sistema", "cantidad"),
            toMapList(ticketRepository.countByTipoGroup(), "tipo", "cantidad"),
            toMapList(ticketRepository.tendenciasMensuales(LocalDateTime.now().minusMonths(12)), "mes", "cantidad")
        );
    }

    private double calcularCumplimientoSLA() {
        return 85.0;
    }

    private List<Map<String, Object>> toMapList(List<Object[]> data, String key1, String key2) {
        return data.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(key1, row[0]);
            map.put(key2, row[1]);
            return map;
        }).collect(Collectors.toList());
    }
}
