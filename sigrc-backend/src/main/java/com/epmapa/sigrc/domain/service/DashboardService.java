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

    public DashboardDTO obtenerDashboard(Integer idUsuario, boolean esAdmin) {
        long abiertos;
        long cerrados;
        long vencidos;
        long sinAsignar;
        double tiempoPromedio;
        long totalDocs;
        long docsVencidos;
        long docsConTicket;
        double docsTiempoProm;
        long docsPendientes;
        long cambiosSol;
        long cambiosAprob;
        long cambiosComp;
        List<Map<String, Object>> memosPendientes;
        List<Object[]> ticketsPorEstado, ticketsPorPrioridad, ticketsPorArea, ticketsPorSistema, ticketsPorTipo;
        List<Object[]> documentosPorEstado, documentosPorPrioridad;
        List<Object[]> cambiosPorEstado, cambiosPorImpacto;
        List<Object[]> tendenciasMensuales;

        if (esAdmin) {
            abiertos = ticketRepository.contarAbiertos(List.of("CERRADO", "RECHAZADO"));
            cerrados = ticketRepository.contarCerrados(List.of("CERRADO", "RECHAZADO"));
            vencidos = ticketRepository.countVencidos();
            sinAsignar = ticketRepository.countByEstado("NUEVO");
            tiempoPromedio = Optional.ofNullable(ticketRepository.avgTiempoAtencionHoras()).orElse(0.0);
            totalDocs = correspondenciaRepository.countActivos();
            docsVencidos = correspondenciaRepository.findVencidos().size();
            docsConTicket = correspondenciaRepository.countQueGeneraronTicket();
            docsTiempoProm = Optional.ofNullable(correspondenciaRepository.tiempoPromedioRespuestaHoras()).orElse(0.0);
            docsPendientes = correspondenciaRepository.findMemosPendientes().size();
            cambiosSol = cambioRepository.countSolicitados();
            cambiosAprob = cambioRepository.countAprobados();
            cambiosComp = cambioRepository.countCompletados();
            memosPendientes = buildMemos(correspondenciaRepository.findMemosPendientes());
            ticketsPorEstado = ticketRepository.countByEstadoGroup();
            ticketsPorPrioridad = ticketRepository.countByPrioridadGroup();
            ticketsPorArea = ticketRepository.countByAreaGroup();
            ticketsPorSistema = ticketRepository.countBySistemaGroup();
            ticketsPorTipo = ticketRepository.countByTipoGroup();
            documentosPorEstado = correspondenciaRepository.countByEstado();
            documentosPorPrioridad = correspondenciaRepository.countByPrioridad();
            cambiosPorEstado = cambioRepository.countByEstadoGroup();
            cambiosPorImpacto = cambioRepository.countByImpactoGroup();
            tendenciasMensuales = ticketRepository.tendenciasMensuales(LocalDateTime.now().minusMonths(12));
        } else {
            abiertos = ticketRepository.contarAbiertosPorUsuario(List.of("CERRADO", "RECHAZADO"), idUsuario);
            cerrados = ticketRepository.contarCerradosPorUsuario(List.of("CERRADO", "RECHAZADO"), idUsuario);
            vencidos = ticketRepository.countVencidosPorUsuario(idUsuario);
            sinAsignar = ticketRepository.countNuevosPorUsuario(idUsuario);
            tiempoPromedio = Optional.ofNullable(ticketRepository.avgTiempoAtencionHorasPorUsuario(idUsuario)).orElse(0.0);
            totalDocs = correspondenciaRepository.countActivosPorUsuario(idUsuario);
            docsVencidos = correspondenciaRepository.findVencidosPorUsuario(idUsuario).size();
            docsConTicket = correspondenciaRepository.countQueGeneraronTicketPorUsuario(idUsuario);
            docsTiempoProm = Optional.ofNullable(correspondenciaRepository.tiempoPromedioRespuestaHorasPorUsuario(idUsuario)).orElse(0.0);
            docsPendientes = correspondenciaRepository.findMemosPendientesPorUsuario(idUsuario).size();
            cambiosSol = cambioRepository.countSolicitadosPorUsuario(idUsuario);
            cambiosAprob = cambioRepository.countAprobadosPorUsuario(idUsuario);
            cambiosComp = cambioRepository.countCompletadosPorUsuario(idUsuario);
            memosPendientes = buildMemos(correspondenciaRepository.findMemosPendientesPorUsuario(idUsuario));
            ticketsPorEstado = ticketRepository.countByEstadoGroupPorUsuario(idUsuario);
            ticketsPorPrioridad = ticketRepository.countByPrioridadGroupPorUsuario(idUsuario);
            ticketsPorArea = ticketRepository.countByAreaGroupPorUsuario(idUsuario);
            ticketsPorSistema = ticketRepository.countBySistemaGroupPorUsuario(idUsuario);
            ticketsPorTipo = ticketRepository.countByTipoGroupPorUsuario(idUsuario);
            documentosPorEstado = correspondenciaRepository.countByEstadoPorUsuario(idUsuario);
            documentosPorPrioridad = correspondenciaRepository.countByPrioridadPorUsuario(idUsuario);
            cambiosPorEstado = cambioRepository.countByEstadoGroupPorUsuario(idUsuario);
            cambiosPorImpacto = cambioRepository.countByImpactoGroupPorUsuario(idUsuario);
            tendenciasMensuales = ticketRepository.tendenciasMensualesPorUsuario(LocalDateTime.now().minusMonths(12), idUsuario);
        }

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

        return new DashboardDTO(
            abiertos, cerrados, vencidos, sinAsignar, tiempoPromedio,
            calcularCumplimientoSLA(),
            totalDocs, docsPendientes, docsVencidos, docsConTicket, docsTiempoProm,
            cambiosSol, cambiosAprob, cambiosComp,
            versionActual, sistemaReciente, ultimoCambioDesc, fechaUltimoCambio,
            memosPendientes,
            toMapList(ticketsPorEstado, "estado", "cantidad"),
            toMapList(ticketsPorPrioridad, "prioridad", "cantidad"),
            toMapList(ticketsPorArea, "area", "cantidad"),
            toMapList(ticketsPorSistema, "sistema", "cantidad"),
            toMapList(ticketsPorTipo, "tipo", "cantidad"),
            toMapList(documentosPorEstado, "estado", "cantidad"),
            toMapList(documentosPorPrioridad, "prioridad", "cantidad"),
            toMapList(cambiosPorEstado, "estado", "cantidad"),
            toMapList(cambiosPorImpacto, "impacto", "cantidad"),
            toMapList(tendenciasMensuales, "mes", "cantidad")
        );
    }

    private List<Map<String, Object>> buildMemos(List<Correspondencia> memos) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Correspondencia m : memos) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getIdCorrespondencia());
            map.put("numeroInterno", m.getNumeroInterno());
            map.put("asunto", m.getAsunto());
            map.put("prioridad", m.getPrioridad());
            map.put("fechaLimite", m.getFechaLimiteRespuesta() != null ? m.getFechaLimiteRespuesta().format(dtf) : "");
            map.put("codigoDocumento", m.getCodigoDocumento());
            map.put("departamentoRemitente", m.getDepartamentoRemitente());
            result.add(map);
        }
        return result;
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

    public List<TicketResumenDTO> listarTicketsAbiertos(Integer idUsuario, boolean esAdmin) {
        List<Ticket> tickets = esAdmin ? ticketRepository.findAbiertos()
                : ticketRepository.findAbiertosPorUsuario(idUsuario);
        return tickets.stream().map(this::toTicketResumen).collect(Collectors.toList());
    }

    public List<TicketResumenDTO> listarTicketsCerrados(Integer idUsuario, boolean esAdmin) {
        List<Ticket> tickets = esAdmin ? ticketRepository.findCerrados()
                : ticketRepository.findCerradosPorUsuario(idUsuario);
        return tickets.stream().map(this::toTicketResumen).collect(Collectors.toList());
    }

    public List<TicketResumenDTO> listarTicketsVencidos(Integer idUsuario, boolean esAdmin) {
        List<Ticket> tickets = esAdmin ? ticketRepository.findVencidosActivos()
                : ticketRepository.findVencidosActivosPorUsuario(idUsuario);
        return tickets.stream().map(this::toTicketResumen).collect(Collectors.toList());
    }

    public List<TicketResumenDTO> listarTicketsSinAsignar(Integer idUsuario, boolean esAdmin) {
        List<Ticket> tickets = esAdmin ? ticketRepository.findByEstadoOrderByCreadoEnDesc("NUEVO")
                : ticketRepository.findNuevosPorSolicitante(idUsuario);
        return tickets.stream().map(this::toTicketResumen).collect(Collectors.toList());
    }

    public List<DocumentoResumenDTO> listarDocumentosPendientes(Integer idUsuario, boolean esAdmin) {
        List<Correspondencia> docs = esAdmin ? correspondenciaRepository.findMemosPendientes()
                : correspondenciaRepository.findMemosPendientesPorUsuario(idUsuario);
        return docs.stream().map(this::toDocumentoResumen).collect(Collectors.toList());
    }

    public List<DocumentoResumenDTO> listarDocumentosVencidos(Integer idUsuario, boolean esAdmin) {
        List<Correspondencia> docs = esAdmin ? correspondenciaRepository.findVencidos()
                : correspondenciaRepository.findVencidosPorUsuario(idUsuario);
        return docs.stream().map(this::toDocumentoResumen).collect(Collectors.toList());
    }

    public List<DocumentoResumenDTO> listarDocumentosConTicket(Integer idUsuario, boolean esAdmin) {
        List<Correspondencia> docs = esAdmin ? correspondenciaRepository.findQueGeneraronTicket()
                : correspondenciaRepository.findQueGeneraronTicketPorUsuario(idUsuario);
        return docs.stream().map(this::toDocumentoResumen).collect(Collectors.toList());
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
