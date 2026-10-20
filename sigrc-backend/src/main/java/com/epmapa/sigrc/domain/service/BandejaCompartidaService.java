package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.BandejaCompartidaDTO;
import com.epmapa.sigrc.domain.dto.CorrespondenciaDTO;
import com.epmapa.sigrc.domain.entity.Correspondencia;
import com.epmapa.sigrc.domain.entity.CorrespondenciaLecturaUsuario;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * BCC — Bandeja compartida: la secretaria consulta los documentos
 * de sus titulares vinculados, con lectura personal independiente.
 */
@Service
public class BandejaCompartidaService {

    private final CorrespondenciaVinculoRepository vinculoRepository;
    private final CorrespondenciaRepository correspondenciaRepository;
    private final CorrespondenciaLecturaUsuarioRepository lecturaRepository;
    private final CorrespondenciaDestinatarioRepository destinatarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final AuditoriaEventos auditoriaEventos;

    // Necesitamos toDTO del servicio principal
    private final CorrespondenciaService correspondenciaService;

    public BandejaCompartidaService(CorrespondenciaVinculoRepository vinculoRepository,
                                    CorrespondenciaRepository correspondenciaRepository,
                                    CorrespondenciaLecturaUsuarioRepository lecturaRepository,
                                    CorrespondenciaDestinatarioRepository destinatarioRepository,
                                    UsuarioRepository usuarioRepository,
                                    AsignacionPuestoRepository asignacionRepository,
                                    AuditoriaEventos auditoriaEventos,
                                    CorrespondenciaService correspondenciaService) {
        this.vinculoRepository = vinculoRepository;
        this.correspondenciaRepository = correspondenciaRepository;
        this.lecturaRepository = lecturaRepository;
        this.destinatarioRepository = destinatarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.auditoriaEventos = auditoriaEventos;
        this.correspondenciaService = correspondenciaService;
    }

    /**
     * Lista las bandejas compartidas disponibles para el asistente.
     * Devuelve un DTO por cada puesto titular vinculado vigente.
     */
    @Transactional(readOnly = true)
    public List<BandejaCompartidaDTO> listarMisBandejas(Integer idUsuario) {
        Integer idPuesto = resolverPuestoVigente(idUsuario);
        if (idPuesto == null) return List.of();

        var hoy = LocalDate.now();
        var vinculos = vinculoRepository.findVigentesComoAsistente(idPuesto, hoy);

        return vinculos.stream()
                .map(v -> {
                    var titular = usuarioRepository
                            .findByPuestoVigente(v.getPuestoTitular().getIdPuesto())
                            .stream().findFirst().orElse(null);
                    String nombreTitular = titular != null
                            ? titular.getNombres() + " " + titular.getApellidos()
                            : v.getNombreTitular();
                    long noLeidos = 0L;
                    try {
                        noLeidos = contarNoLeidos(v.getPuestoTitular().getIdPuesto(), idUsuario);
                    } catch (Exception ignored) {
                        // vínculo recién creado / sin documentos
                    }
                    return new BandejaCompartidaDTO(
                            v.getIdVinculo(),
                            v.getPuestoTitular().getIdPuesto(),
                            v.getNombreTitular(),
                            nombreTitular,
                            List.of(),
                            (int) noLeidos
                    );
                })
                .toList();
    }

    /**
     * IDs de documentos ya leídos por el usuario autenticado, para un
     * subconjunto de documentos (evita N+1 en el frontend).
     */
    @Transactional(readOnly = true)
    public List<Integer> lecturasDe(List<Integer> idsCorrespondencia, Integer idUsuario) {
        if (idsCorrespondencia == null || idsCorrespondencia.isEmpty()) return List.of();
        return lecturaRepository
                .findByCorrespondenciaIdCorrespondenciaInAndUsuarioId(idsCorrespondencia, idUsuario)
                .stream()
                .map(l -> l.getCorrespondencia().getIdCorrespondencia())
                .toList();
    }

    /**
     * Documentos de la bandeja de un puesto titular específico,
     * visibles para el asistente vinculado.
     */
    @Transactional(readOnly = true)
    public List<CorrespondenciaDTO> bandejaTitular(Integer idPuestoTitular, Integer idUsuarioAsistente) {
        verificarVinculo(idUsuarioAsistente, idPuestoTitular);

        var usuariosTitular = usuarioRepository.findByPuestoVigente(idPuestoTitular);
        if (usuariosTitular.isEmpty()) return List.of();

        var idsUsuarios = usuariosTitular.stream()
                .map(u -> u.getIdUsuario())
                .toList();

        // Documentos donde el titular participa (creador, responsable o destinatario)
        var documentos = correspondenciaRepository
                .findByUsuariosParticipanOrderByCreadoEnDesc(idsUsuarios);

        return documentos.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Marca un documento como leído POR ESTE USUARIO (no global).
     * No cambia el contador de no leídos del titular.
     */
    @Transactional
    public void marcarLeidoPersonal(Integer idCorrespondencia, Integer idUsuario) {
        var documento = correspondenciaRepository.findById(idCorrespondencia)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));

        if (!lecturaRepository.existsByCorrespondenciaIdCorrespondenciaAndUsuarioId(
                idCorrespondencia, idUsuario)) {
            var lectura = CorrespondenciaLecturaUsuario.builder()
                    .correspondencia(documento)
                    .usuarioId(idUsuario)
                    .build();
            lecturaRepository.save(lectura);
        }

        auditoriaEventos.registrar("LEER_DOCUMENTO_BANDEJA_COMPARTIDA", "CONSULTA",
                "correspondencia", idCorrespondencia,
                null, null, "OK");
    }

    /**
     * Verifica si el documento fue leído por un usuario específico.
     */
    @Transactional(readOnly = true)
    public boolean fueLeidoPor(Integer idCorrespondencia, Integer idUsuario) {
        return lecturaRepository
                .existsByCorrespondenciaIdCorrespondenciaAndUsuarioId(idCorrespondencia, idUsuario);
    }

    /**
     * Cuenta documentos no leídos por el asistente en la bandeja del titular.
     */
    @Transactional(readOnly = true)
    public long contarNoLeidos(Integer idPuestoTitular, Integer idUsuarioAsistente) {
        verificarVinculo(idUsuarioAsistente, idPuestoTitular);

        var usuariosTitular = usuarioRepository.findByPuestoVigente(idPuestoTitular);
        if (usuariosTitular.isEmpty()) return 0;

        var idsUsuarios = usuariosTitular.stream().map(u -> u.getIdUsuario()).toList();
        var documentos = correspondenciaRepository
                .findByUsuariosParticipanOrderByCreadoEnDesc(idsUsuarios);

        return documentos.stream()
                .filter(d -> !lecturaRepository.existsByCorrespondenciaIdCorrespondenciaAndUsuarioId(
                        d.getIdCorrespondencia(), idUsuarioAsistente))
                .count();
    }

    /**
     * Verifica si el usuario puede consultar el seguimiento de un documento:
     * por ser participante directo (creador, responsable o destinatario) o
     * por tener vínculo de asistente con permiso de seguimiento hacia un
     * titular que participa del documento.
     */
    @Transactional(readOnly = true)
    public boolean puedeSeguir(Integer idUsuario, Integer idCorrespondencia) {
        var documento = correspondenciaRepository.findById(idCorrespondencia).orElse(null);
        if (documento == null || !Boolean.TRUE.equals(documento.getActivo())) return false;

        boolean participa =
            (documento.getCreadoPor() != null
                && idUsuario.equals(documento.getCreadoPor().getIdUsuario()))
            || documento.getResponsablesAsignados().stream()
                .anyMatch(ra -> ra.getUsuario() != null
                    && idUsuario.equals(ra.getUsuario().getIdUsuario()))
            || destinatarioRepository.findByCorrespondenciaIdCorrespondencia(idCorrespondencia).stream()
                .anyMatch(d -> "USUARIO".equals(d.getTipo())
                    && idUsuario.equals(d.getIdDestinatario()));
        if (participa) return true;

        Integer idPuestoAsistente = resolverPuestoVigente(idUsuario);
        if (idPuestoAsistente == null) return false;

        Set<Integer> participantes = new LinkedHashSet<>();
        if (documento.getCreadoPor() != null) participantes.add(documento.getCreadoPor().getIdUsuario());
        documento.getResponsablesAsignados().stream()
            .map(ra -> ra.getUsuario() != null ? ra.getUsuario().getIdUsuario() : null)
            .filter(java.util.Objects::nonNull)
            .forEach(participantes::add);
        if (participantes.isEmpty()) return false;

        for (var v : vinculoRepository.findVigentesComoAsistente(idPuestoAsistente, LocalDate.now())) {
            if (!Boolean.TRUE.equals(v.getPuedeSeguimiento())) continue;
            boolean coincide = usuarioRepository
                .findByPuestoVigente(v.getPuestoTitular().getIdPuesto()).stream()
                .anyMatch(u -> participantes.contains(u.getIdUsuario()));
            if (coincide) return true;
        }
        return false;
    }

    /**
     * Verifica que el asistente tenga vínculo vigente con el titular.
     */
    private void verificarVinculo(Integer idUsuarioAsistente, Integer idPuestoTitular) {
        Integer idPuestoAsistente = resolverPuestoVigente(idUsuarioAsistente);
        if (idPuestoAsistente == null)
            throw new SecurityException("El usuario no tiene asignación de puesto vigente");

        var hoy = LocalDate.now();
        var vinculo = vinculoRepository
                .findVigenteEntre(idPuestoAsistente, idPuestoTitular, hoy)
                .orElseThrow(() -> new SecurityException(
                    "No tiene vínculo vigente con el puesto titular " + idPuestoTitular));
        if (!Boolean.TRUE.equals(vinculo.getPuedeSeguimiento()))
            throw new SecurityException("El vínculo no autoriza seguimiento");
    }

    private Integer resolverPuestoVigente(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getEmpleado() == null) return null;
        return asignacionRepository
                .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                        usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
                .map(a -> a.getPuesto() != null ? a.getPuesto().getIdPuesto() : null)
                .orElse(null);
    }

    private CorrespondenciaDTO toDTO(Correspondencia entity) {
        // Delegate al servicio principal para mantener consistencia
        return correspondenciaService.obtener(entity.getIdCorrespondencia());
    }
}
