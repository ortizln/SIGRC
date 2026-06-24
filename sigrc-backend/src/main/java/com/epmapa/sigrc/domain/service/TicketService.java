package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistorialRepository historialRepository;
    private final TicketComentarioRepository comentarioRepository;
    private final TicketAdjuntoRepository adjuntoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final SistemaRepository sistemaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

    public TicketService(TicketRepository ticketRepository,
                         TicketHistorialRepository historialRepository,
                         TicketComentarioRepository comentarioRepository,
                         TicketAdjuntoRepository adjuntoRepository,
                         UsuarioRepository usuarioRepository,
                         AreaRepository areaRepository,
                         SistemaRepository sistemaRepository,
                         CategoriaRepository categoriaRepository,
                         SubcategoriaRepository subcategoriaRepository) {
        this.ticketRepository = ticketRepository;
        this.historialRepository = historialRepository;
        this.comentarioRepository = comentarioRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
        this.sistemaRepository = sistemaRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
    }

    public PaginacionDTO<TicketDTO> listar(int pagina, int tamanio, String estado, String tipo,
                                            String prioridad, Integer idSolicitante,
                                            Integer idResponsable, Integer idArea,
                                            Integer idSistema, String texto) {
        var pageable = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "creadoEn"));
        Page<Ticket> result = ticketRepository.buscar(estado, tipo, prioridad,
            idSolicitante, idResponsable, idArea, idSistema, texto, pageable);
        return toPaginacion(result);
    }

    public TicketDTO obtenerPorId(Integer id) {
        return toDTO(ticketRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado: " + id)));
    }

    @Transactional
    public TicketDTO crear(TicketCrearRequest request) {
        var ticket = Ticket.builder()
            .tipo(request.tipo())
            .prioridad(request.prioridad())
            .estado("NUEVO")
            .asunto(request.asunto())
            .descripcion(request.descripcion())
            .impacto(request.impacto())
            .urgencia(request.urgencia())
            .origen(request.origen() != null ? request.origen() : "SISTEMA")
            .solicitante(usuarioRepository.getReferenceById(request.idSolicitante()))
            .area(areaRepository.getReferenceById(request.idArea()))
            .activo(true)
            .numeroReaperturas(0)
            .esReabierto(false)
            .creadoEn(LocalDateTime.now())
            .actualizadoEn(LocalDateTime.now())
            .build();

        if (request.idSistema() != null)
            ticket.setSistema(sistemaRepository.getReferenceById(request.idSistema()));
        if (request.idCategoria() != null)
            ticket.setCategoria(categoriaRepository.getReferenceById(request.idCategoria()));
        if (request.idSubcategoria() != null)
            ticket.setSubcategoria(subcategoriaRepository.getReferenceById(request.idSubcategoria()));

        ticket = ticketRepository.save(ticket);

        registrarHistorial(ticket.getIdTicket(), null, "NUEVO", request.idSolicitante(),
            "Creación de ticket");

        return toDTO(ticket);
    }

    @Transactional
    public TicketDTO actualizarEstado(Integer idTicket, String estadoNuevo,
                                       Integer idUsuario, String observacion) {
        var ticket = ticketRepository.findById(idTicket)
            .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado: " + idTicket));
        String estadoAnterior = ticket.getEstado();
        ticket.setEstado(estadoNuevo);

        if ("CERRADO".equals(estadoNuevo)) {
            ticket.setFechaCierre(LocalDateTime.now());
        }
        if ("RESUELTO".equals(estadoNuevo)) {
            ticket.setFechaCierre(LocalDateTime.now());
        }
        if ("RECHAZADO".equals(estadoNuevo)) {
            ticket.setFechaCierre(LocalDateTime.now());
        }

        ticket = ticketRepository.save(ticket);
        registrarHistorial(idTicket, estadoAnterior, estadoNuevo, idUsuario, observacion);

        return toDTO(ticket);
    }

    @Transactional
    public TicketDTO asignarResponsable(Integer idTicket, Integer idResponsable, Integer idUsuario) {
        var ticket = ticketRepository.findById(idTicket)
            .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado: " + idTicket));
        var responsable = usuarioRepository.findById(idResponsable)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idResponsable));
        ticket.setResponsable(responsable);
        if ("NUEVO".equals(ticket.getEstado())) {
            ticket.setEstado("ASIGNADO");
        }
        ticket = ticketRepository.save(ticket);
        registrarHistorial(idTicket, null, ticket.getEstado(), idUsuario,
            "Asignado a: " + responsable.getNombres() + " " + responsable.getApellidos());
        return toDTO(ticket);
    }

    @Transactional
    public TicketDTO actualizarTicket(Integer id, TicketCrearRequest request) {
        var ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado: " + id));
        if (request.tipo() != null) ticket.setTipo(request.tipo());
        if (request.prioridad() != null) ticket.setPrioridad(request.prioridad());
        if (request.asunto() != null) ticket.setAsunto(request.asunto());
        if (request.descripcion() != null) ticket.setDescripcion(request.descripcion());
        if (request.impacto() != null) ticket.setImpacto(request.impacto());
        if (request.urgencia() != null) ticket.setUrgencia(request.urgencia());
        if (request.idArea() != null)
            ticket.setArea(areaRepository.getReferenceById(request.idArea()));
        if (request.idSistema() != null)
            ticket.setSistema(sistemaRepository.getReferenceById(request.idSistema()));
        if (request.idCategoria() != null)
            ticket.setCategoria(categoriaRepository.getReferenceById(request.idCategoria()));
        if (request.idSubcategoria() != null)
            ticket.setSubcategoria(subcategoriaRepository.getReferenceById(request.idSubcategoria()));
        return toDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public void eliminar(Integer id) {
        var ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado: " + id));
        ticket.setActivo(false);
        ticketRepository.save(ticket);
    }

    public Ticket getReferenceById(Integer idTicket) {
        return ticketRepository.getReferenceById(idTicket);
    }

    private void registrarHistorial(Integer idTicket, String anterior, String nuevo,
                                     Integer idUsuario, String observacion) {
        var historial = TicketHistorial.builder()
            .ticket(Ticket.builder().idTicket(idTicket).build())
            .estadoAnterior(anterior)
            .estadoNuevo(nuevo)
            .usuario(Usuario.builder().idUsuario(idUsuario).build())
            .observacion(observacion)
            .creadoEn(LocalDateTime.now())
            .build();
        historialRepository.save(historial);
    }

    public List<TicketComentario> obtenerComentarios(Integer idTicket) {
        return comentarioRepository.findByTicket_IdTicketOrderByCreadoEnDesc(idTicket);
    }

    @Transactional
    public TicketComentario agregarComentario(Integer idTicket, Integer idUsuario,
                                               String comentario, boolean esInterno) {
        var entity = TicketComentario.builder()
            .ticket(Ticket.builder().idTicket(idTicket).build())
            .usuario(Usuario.builder().idUsuario(idUsuario).build())
            .comentario(comentario)
            .esInterno(esInterno)
            .creadoEn(LocalDateTime.now())
            .build();
        return comentarioRepository.save(entity);
    }

    public PaginacionDTO<TicketDTO> toPaginacion(Page<Ticket> page) {
        List<TicketDTO> contenido = page.getContent().stream()
            .map(this::toDTO).collect(Collectors.toList());
        return new PaginacionDTO<>(contenido, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast());
    }

    public TicketDTO toDTO(Ticket t) {
        return new TicketDTO(
            t.getIdTicket(), t.getNumeroTicket(), t.getTipo(), t.getEstado(),
            t.getPrioridad(), t.getAsunto(), t.getDescripcion(),
            t.getImpacto(), t.getUrgencia(), t.getOrigen(),
            t.getFechaLimite(), t.getFechaCierre(),
            t.getCreadoEn(), t.getActualizadoEn(),
            t.getCausaRaiz(), t.getSolucion(), t.getEsReabierto(),
            t.getNumeroReaperturas(), t.getCalificacion(), t.getComentarioCierre(),
            t.getSolicitante().getIdUsuario(),
            t.getSolicitante().getUsername(),
            t.getSolicitante().getNombres() + " " + t.getSolicitante().getApellidos(),
            t.getArea().getIdArea(), t.getArea().getNombre(),
            t.getSistema() != null ? t.getSistema().getIdSistema() : null,
            t.getSistema() != null ? t.getSistema().getNombre() : null,
            t.getCategoria() != null ? t.getCategoria().getIdCategoria() : null,
            t.getCategoria() != null ? t.getCategoria().getNombre() : null,
            t.getSubcategoria() != null ? t.getSubcategoria().getIdSubcategoria() : null,
            t.getSubcategoria() != null ? t.getSubcategoria().getNombre() : null,
            t.getResponsable() != null ? t.getResponsable().getIdUsuario() : null,
            t.getResponsable() != null ? t.getResponsable().getUsername() : null,
            t.getResponsable() != null ? t.getResponsable().getNombres() + " " + t.getResponsable().getApellidos() : null,
            t.getSla() != null ? t.getSla().getIdSla() : null,
            t.getSla() != null ? t.getSla().getNombre() : null,
            formatearEstado(t.getEstado())
        );
    }

    private String formatearEstado(String estado) {
        return switch (estado) {
            case "NUEVO" -> "Nuevo";
            case "ASIGNADO" -> "Asignado";
            case "EN_ANALISIS" -> "En Análisis";
            case "EN_DESARROLLO" -> "En Desarrollo";
            case "EN_PRUEBAS" -> "En Pruebas";
            case "PENDIENTE_USUARIO" -> "Pendiente Usuario";
            case "RESUELTO" -> "Resuelto";
            case "CERRADO" -> "Cerrado";
            case "RECHAZADO" -> "Rechazado";
            default -> estado;
        };
    }
}
