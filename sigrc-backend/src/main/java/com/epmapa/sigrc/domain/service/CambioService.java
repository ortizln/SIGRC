package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.CambioDTO;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CambioService {

    private final CambioRepository cambioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SistemaRepository sistemaRepository;
    private final TicketRepository ticketRepository;
    private final NotificacionWebSocketService notificacionService;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    public CambioService(CambioRepository cambioRepository, UsuarioRepository usuarioRepository,
                         SistemaRepository sistemaRepository, TicketRepository ticketRepository,
                         NotificacionWebSocketService notificacionService) {
        this.cambioRepository = cambioRepository;
        this.usuarioRepository = usuarioRepository;
        this.sistemaRepository = sistemaRepository;
        this.ticketRepository = ticketRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public List<CambioDTO> listar() {
        return cambioRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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

        var entity = cambioRepository.save(cambio);
        var result = toDTO(entity);
        return result;
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
    public void subirPlanArchivo(Integer idCambio, MultipartFile file) {
        try {
            var cambio = cambioRepository.findById(idCambio)
                .orElseThrow(() -> new EntityNotFoundException("Cambio no encontrado: " + idCambio));
            var uploadDir = Paths.get(uploadPath, "cambios", idCambio.toString());
            Files.createDirectories(uploadDir);
            var extension = "";
            var nombreOriginal = file.getOriginalFilename();
            if (nombreOriginal != null && nombreOriginal.contains(".")) {
                extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            }
            var nombreArchivo = UUID.randomUUID().toString() + extension;
            Files.copy(file.getInputStream(), uploadDir.resolve(nombreArchivo));
            cambio.setPlanArchivo(nombreArchivo + "|" + (nombreOriginal != null ? nombreOriginal : "plan"));
            cambioRepository.save(cambio);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo de planificación", e);
        }
    }

    public Resource descargarPlanArchivo(Integer idCambio) {
        var cambio = cambioRepository.findById(idCambio)
            .orElseThrow(() -> new EntityNotFoundException("Cambio no encontrado: " + idCambio));
        if (cambio.getPlanArchivo() == null || cambio.getPlanArchivo().isBlank())
            throw new RuntimeException("El cambio no tiene archivo de planificación");
        var parts = cambio.getPlanArchivo().split("\\|", 2);
        try {
            var ruta = Paths.get(uploadPath, "cambios", idCambio.toString(), parts[0]);
            var resource = new UrlResource(ruta.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new RuntimeException("No se pudo leer el archivo de planificación");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error al leer archivo de planificación", e);
        }
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
            c.getPlanImplementacion(), c.getPlanRetorno(), c.getPlanArchivo(),
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
