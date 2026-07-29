package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.DashboardDTO;
import com.epmapa.sigrc.domain.dto.DocumentoResumenDTO;
import com.epmapa.sigrc.domain.dto.TicketResumenDTO;
import com.epmapa.sigrc.domain.entity.Correspondencia;
import com.epmapa.sigrc.domain.entity.Ticket;
import com.epmapa.sigrc.domain.entity.Version;
import com.epmapa.sigrc.domain.repository.CambioRepository;
import com.epmapa.sigrc.domain.repository.CorrespondenciaRepository;
import com.epmapa.sigrc.domain.repository.TicketRepository;
import com.epmapa.sigrc.domain.repository.VersionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final CorrespondenciaRepository correspondenciaRepository;
    private final CambioRepository cambioRepository;
    private final VersionRepository versionRepository;

    public DashboardService(TicketRepository ticketRepository,
                            CorrespondenciaRepository correspondenciaRepository,
                            CambioRepository cambioRepository,
                            VersionRepository versionRepository) {
        this.ticketRepository = ticketRepository;
        this.correspondenciaRepository = correspondenciaRepository;
        this.cambioRepository = cambioRepository;
        this.versionRepository = versionRepository;
    }

    public DashboardDTO obtenerDashboard() {
        long abiertos = ticketRepository.contarAbiertos(List.of("CERRADO", "RECHAZADO"));
        long cerrados = ticketRepository.contarCerrados(List.of("CERRADO", "RECHAZADO"));
        long vencidos = ticketRepository.countVencidos();
        long sinAsignar = ticketRepository.countByEstado("NUEVO");
        double tiempoPromedio = Optional.ofNullable(ticketRepository.avgTiempoAtencionHoras()).orElse(0.0);

        long totalDocs = correspondenciaRepository.countActivos();
        long docsVencidos = correspondenciaRepository.findVencidos().size();
        long docsConTicket = correspondenciaRepository.countQueGeneraronTicket();
        double docsTiempoProm = Optional.ofNullable(correspondenciaRepository.tiempoPromedioRespuestaHoras()).orElse(0.0);
        long docsPendientes = correspondenciaRepository.findMemosPendientes().size();

        long cambiosSol = cambioRepository.countSolicitados();
        long cambiosAprob = cambioRepository.countAprobados();
        long cambiosComp = cambioRepository.countCompletados();

        String versionActual = "";
        String sistemaReciente = "";
        Optional<Version> ultimaVersion = versionRepository.findTopByActivoTrueAndEstadoDesplegadoOrderByFechaDespliegueDesc();
        if (ultimaVersion.isPresent()) {
            Version v = ultimaVersion.get();
            versionActual = v.getVersion();
            sistemaReciente = v.getSistema() != null ? v.getSistema().getNombre() : "";
        }

        String ultimoCambioDesc = "";
        String fechaUltimoCambio = "";
        var ultimoCambio = cambioRepository.findTopByActivoTrueOrderByCreadoEnDesc();
        if (ultimoCambio.isPresent()) {
            var c = ultimoCambio.get();
            ultimoCambioDesc = c.getTitulo();
            fechaUltimoCambio = c.getCreadoEn() != null
                    ? c.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "";
        }

        List<Map<String, Object>> memosPendientes = new ArrayList<>();
        List<Correspondencia> memos = correspondenciaRepository.findMemosPendientes();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Correspondencia m : memos) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getIdCorrespondencia());
            map.put("numeroInterno", m.getNumeroInterno());
            map.put("asunto", m.getAsunto());
            map.put("prioridad", m.getPrioridad());
            map.put("fechaLimite", m.getFechaLimiteRespuesta() != null ? m.getFechaLimiteRespuesta().format(dtf) : "");
            map.put("codigoDocumento", m.getCodigoDocumento());
            map.put("departamentoRemitente", m.getDepartamentoRemitente());
            memosPendientes.add(map);
        }

        return new DashboardDTO(
            abiertos, cerrados, vencidos, sinAsignar, tiempoPromedio,
            calcularCumplimientoSLA(),
            totalDocs, docsPendientes, docsVencidos, docsConTicket, docsTiempoProm,
            cambiosSol, cambiosAprob, cambiosComp,
            versionActual, sistemaReciente, ultimoCambioDesc, fechaUltimoCambio,
            memosPendientes,
            toMapList(ticketRepository.countByEstadoGroup(), "estado", "cantidad"),
            toMapList(ticketRepository.countByPrioridadGroup(), "prioridad", "cantidad"),
            toMapList(ticketRepository.countByAreaGroup(), "area", "cantidad"),
            toMapList(ticketRepository.countBySistemaGroup(), "sistema", "cantidad"),
            toMapList(ticketRepository.countByTipoGroup(), "tipo", "cantidad"),
            toMapList(correspondenciaRepository.countByEstado(), "estado", "cantidad"),
            toMapList(correspondenciaRepository.countByPrioridad(), "prioridad", "cantidad"),
            toMapList(cambioRepository.countByEstadoGroup(), "estado", "cantidad"),
            toMapList(cambioRepository.countByImpactoGroup(), "impacto", "cantidad"),
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

    public List<TicketResumenDTO> listarTicketsAbiertos() {
        return ticketRepository.findAbiertos().stream()
                .map(this::toTicketResumen)
                .collect(Collectors.toList());
    }

    public List<TicketResumenDTO> listarTicketsCerrados() {
        return ticketRepository.findCerrados().stream()
                .map(this::toTicketResumen)
                .collect(Collectors.toList());
    }

    public List<TicketResumenDTO> listarTicketsVencidos() {
        return ticketRepository.findVencidosActivos().stream()
                .map(this::toTicketResumen)
                .collect(Collectors.toList());
    }

    public List<TicketResumenDTO> listarTicketsSinAsignar() {
        return ticketRepository.findByEstadoOrderByCreadoEnDesc("NUEVO").stream()
                .map(this::toTicketResumen)
                .collect(Collectors.toList());
    }

    public List<DocumentoResumenDTO> listarDocumentosPendientes() {
        return correspondenciaRepository.findMemosPendientes().stream()
                .map(this::toDocumentoResumen)
                .collect(Collectors.toList());
    }

    public List<DocumentoResumenDTO> listarDocumentosVencidos() {
        return correspondenciaRepository.findVencidos().stream()
                .map(this::toDocumentoResumen)
                .collect(Collectors.toList());
    }

    public List<DocumentoResumenDTO> listarDocumentosConTicket() {
        return correspondenciaRepository.findQueGeneraronTicket().stream()
                .map(this::toDocumentoResumen)
                .collect(Collectors.toList());
    }

    private TicketResumenDTO toTicketResumen(Ticket t) {
        return new TicketResumenDTO(
            t.getIdTicket(),
            t.getNumeroTicket(),
            t.getAsunto(),
            t.getEstado(),
            t.getPrioridad(),
            t.getTipo(),
            t.getCreadoEn() != null ? t.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
        );
    }

    private DocumentoResumenDTO toDocumentoResumen(Correspondencia c) {
        return new DocumentoResumenDTO(
            c.getIdCorrespondencia(),
            c.getNumeroInterno(),
            c.getCodigoDocumento(),
            c.getAsunto(),
            c.getEstado(),
            c.getPrioridad(),
            c.getDepartamentoRemitente(),
            c.getFechaLimiteRespuesta() != null ? c.getFechaLimiteRespuesta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
            c.getCreadoEn() != null ? c.getCreadoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
        );
    }
}
