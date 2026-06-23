package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.CambioDTO;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CambioService {

    private final CambioRepository cambioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SistemaRepository sistemaRepository;
    private final TicketRepository ticketRepository;

    public CambioService(CambioRepository cambioRepository, UsuarioRepository usuarioRepository,
                         SistemaRepository sistemaRepository, TicketRepository ticketRepository) {
        this.cambioRepository = cambioRepository;
        this.usuarioRepository = usuarioRepository;
        this.sistemaRepository = sistemaRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<CambioDTO> listar() {
        return cambioRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CambioDTO obtenerPorId(Integer id) {
        return toDTO(cambioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cambio no encontrado: " + id)));
    }

    @Transactional
    public CambioDTO crear(CambioDTO dto) {
        var cambio = Cambio.builder()
            .titulo(dto.titulo())
            .descripcion(dto.descripcion())
            .justificacion(dto.justificacion())
            .tipo(dto.tipo())
            .impacto(dto.impacto())
            .riesgo(dto.riesgo())
            .estado("SOLICITADO")
            .planImplementacion(dto.planImplementacion())
            .planRetorno(dto.planRetorno())
            .solicitante(usuarioRepository.getReferenceById(dto.idSolicitante()))
            .activo(true)
            .creadoEn(LocalDateTime.now())
            .build();

        if (dto.idSistema() != null)
            cambio.setSistema(sistemaRepository.getReferenceById(dto.idSistema()));
        if (dto.idTicket() != null)
            cambio.setTicket(Ticket.builder().idTicket(dto.idTicket()).build());

        return toDTO(cambioRepository.save(cambio));
    }

    @Transactional
    public CambioDTO aprobar(Integer idCambio, Integer idAprobador) {
        var cambio = cambioRepository.findById(idCambio)
            .orElseThrow(() -> new EntityNotFoundException("Cambio no encontrado: " + idCambio));
        cambio.setEstado("APROBADO");
        cambio.setAprobador(usuarioRepository.getReferenceById(idAprobador));
        cambio.setFechaAprobacion(LocalDateTime.now());
        return toDTO(cambioRepository.save(cambio));
    }

    @Transactional
    public CambioDTO actualizar(Integer id, CambioDTO dto) {
        var cambio = cambioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cambio no encontrado: " + id));
        if (dto.titulo() != null) cambio.setTitulo(dto.titulo());
        if (dto.descripcion() != null) cambio.setDescripcion(dto.descripcion());
        if (dto.justificacion() != null) cambio.setJustificacion(dto.justificacion());
        if (dto.tipo() != null) cambio.setTipo(dto.tipo());
        if (dto.impacto() != null) cambio.setImpacto(dto.impacto());
        if (dto.riesgo() != null) cambio.setRiesgo(dto.riesgo());
        if (dto.planImplementacion() != null) cambio.setPlanImplementacion(dto.planImplementacion());
        if (dto.planRetorno() != null) cambio.setPlanRetorno(dto.planRetorno());
        if (dto.idSistema() != null)
            cambio.setSistema(sistemaRepository.getReferenceById(dto.idSistema()));
        return toDTO(cambioRepository.save(cambio));
    }

    @Transactional
    public void eliminar(Integer id) {
        var cambio = cambioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cambio no encontrado: " + id));
        cambio.setActivo(false);
        cambioRepository.save(cambio);
    }

    private CambioDTO toDTO(Cambio c) {
        return new CambioDTO(
            c.getIdCambio(), c.getCodigoCambio(), c.getTitulo(),
            c.getDescripcion(), c.getJustificacion(), c.getTipo(),
            c.getImpacto(), c.getRiesgo(), c.getEstado(),
            c.getPlanImplementacion(), c.getPlanRetorno(),
            c.getFechaAprobacion(), c.getFechaInicio(), c.getFechaImplementacion(),
            c.getFechaVerificacion(), c.getResultado(), c.getLeccionesAprendidas(),
            c.getCreadoEn(),
            c.getTicket() != null ? c.getTicket().getIdTicket() : null,
            c.getTicket() != null ? c.getTicket().getNumeroTicket() : null,
            c.getSistema() != null ? c.getSistema().getIdSistema() : null,
            c.getSistema() != null ? c.getSistema().getNombre() : null,
            c.getSolicitante().getIdUsuario(),
            c.getSolicitante().getNombres() + " " + c.getSolicitante().getApellidos(),
            c.getAprobador() != null ? c.getAprobador().getIdUsuario() : null,
            c.getAprobador() != null ? c.getAprobador().getNombres() + " " + c.getAprobador().getApellidos() : null,
            c.getResponsable() != null ? c.getResponsable().getIdUsuario() : null,
            c.getResponsable() != null ? c.getResponsable().getNombres() + " " + c.getResponsable().getApellidos() : null
        );
    }
}
