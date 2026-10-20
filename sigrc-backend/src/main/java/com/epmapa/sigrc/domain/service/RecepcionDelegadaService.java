package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.RecepcionDelegadaRequest;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BCC — Recepción delegada por asistente sobre bandeja del titular.
 *
 * Secuencia transaccional:
 *  1. Autenticar y resolver puesto vigente del asistente.
 *  2. Comprobar vínculo vigente asistente→titular.
 *  3. Verificar permiso puede_recepcionar.
 *  4. Comprobar idempotencia.
 *  5. Registrar recepción real + asignación oficial al titular.
 *  6. Historial y evento en la misma transacción.
 *  7. Notificar al titular.
 */
@Service
public class RecepcionDelegadaService {

    private static final Logger log = LoggerFactory.getLogger(RecepcionDelegadaService.class);

    private final CorrespondenciaRepository correspondenciaRepository;
    private final CorrespondenciaDestinatarioRepository destinatarioRepository;
    private final CorrespondenciaHistorialRepository historialRepository;
    private final CorrespondenciaVinculoRepository vinculoRepository;
    private final CorrespondenciaRecepcionIdempotenteRepository idempotenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final NotificacionWebSocketService notificacionService;
    private final AuditoriaEventos auditoriaEventos;

    public RecepcionDelegadaService(CorrespondenciaRepository correspondenciaRepository,
                                    CorrespondenciaDestinatarioRepository destinatarioRepository,
                                    CorrespondenciaHistorialRepository historialRepository,
                                    CorrespondenciaVinculoRepository vinculoRepository,
                                    CorrespondenciaRecepcionIdempotenteRepository idempotenteRepository,
                                    UsuarioRepository usuarioRepository,
                                    AsignacionPuestoRepository asignacionRepository,
                                    NotificacionWebSocketService notificacionService,
                                    AuditoriaEventos auditoriaEventos) {
        this.correspondenciaRepository = correspondenciaRepository;
        this.destinatarioRepository = destinatarioRepository;
        this.historialRepository = historialRepository;
        this.vinculoRepository = vinculoRepository;
        this.idempotenteRepository = idempotenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.notificacionService = notificacionService;
        this.auditoriaEventos = auditoriaEventos;
    }

    /**
     * Ejecuta la recepción delegada. Lanza SecurityException si el
     * asistente no tiene vínculo vigente o permiso.
     */
    @Transactional
    public Correspondencia recepcionar(RecepcionDelegadaRequest req, Integer idUsuarioAsistente) {

        // ── 1. Idempotencia ──
        var existente = idempotenteRepository.findByIdempotencyKey(req.idempotencyKey());
        if (existente.isPresent()) {
            log.info("Recepción delegada idempotente: key={} → doc={}",
                    req.idempotencyKey(), existente.get().getCorrespondenciaId());
            return correspondenciaRepository.findById(existente.get().getCorrespondenciaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                        "Documento no encontrado para clave idempotente"));
        }

        // ── 2. Resolver puesto vigente del asistente ──
        var usuario = usuarioRepository.findById(idUsuarioAsistente)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getEmpleado() == null)
            throw new SecurityException("El usuario no tiene empleado vinculado");

        var asignacionAsistente = asignacionRepository
                .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                        usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
                .orElseThrow(() -> new SecurityException(
                    "El usuario no tiene asignación de puesto vigente"));

        Integer idPuestoAsistente = asignacionAsistente.getPuesto() != null
                ? asignacionAsistente.getPuesto().getIdPuesto() : null;
        if (idPuestoAsistente == null)
            throw new SecurityException("La asignación vigente no tiene puesto asociado");

        // ── 3. Comprobar vínculo vigente ──
        var hoy = java.time.LocalDate.now();
        var vinculo = vinculoRepository
                .findVigenteEntre(idPuestoAsistente, req.puestoTitularId(), hoy)
                .orElseThrow(() -> new SecurityException(
                    "No tiene vínculo vigente con el puesto titular " + req.puestoTitularId()));
        if (!Boolean.TRUE.equals(vinculo.getPuedeRecepcionar()))
            throw new SecurityException("El vínculo no autoriza recepción delegada");

        // ── 4. Verificar clasificación del documento ──
        var documento = correspondenciaRepository.findById(req.correspondenciaId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Documento no encontrado: " + req.correspondenciaId()));
        if (!Boolean.TRUE.equals(documento.getActivo()))
            throw new IllegalArgumentException("El documento está inactivo");

        // ── 5. Registrar recepción real (asistente como receptor) ──
        // Buscar si el documento tiene un destinatario tipo PUESTO para el titular
        // o si hay que asignar al titular como responsable.
        boolean yaRecibido = false;
        var destinatarios = destinatarioRepository
                .findByCorrespondenciaIdCorrespondencia(req.correspondenciaId());
        for (var d : destinatarios) {
            if ("USUARIO".equals(d.getTipo()) && d.getIdDestinatario() != null
                    && d.getIdDestinatario().equals(idUsuarioAsistente)) {
                if (!Boolean.TRUE.equals(d.getRecibido())) {
                    d.setRecibido(true);
                    d.setFechaRecibido(LocalDateTime.now());
                    destinatarioRepository.save(d);
                }
                yaRecibido = true;
            }
        }
        if (!yaRecibido) {
            // Registrar al asistente como destinatario recibido
            var nuevoDestinatario = CorrespondenciaDestinatario.builder()
                    .correspondencia(documento)
                    .tipo("USUARIO")
                    .idDestinatario(idUsuarioAsistente)
                    .nombre(usuario.getNombres() + " " + usuario.getApellidos() + " (Secretaría)")
                    .recibido(true)
                    .fechaRecibido(LocalDateTime.now())
                    .build();
            destinatarioRepository.save(nuevoDestinatario);
        }

        // ── 6. Asignación oficial al titular ──
        // Resolver el usuario del titular desde su puesto vigente
        var titular = resolverUsuarioTitular(req.puestoTitularId());
        if (titular == null)
            throw new SecurityException("No se pudo resolver el usuario titular del puesto " + req.puestoTitularId());

        // Verificar que el titular no esté ya asignado como responsable
        boolean titularYaAsignado = documento.getResponsablesAsignados().stream()
                .anyMatch(ra -> ra.getUsuario() != null
                        && ra.getUsuario().getIdUsuario().equals(titular.getIdUsuario()));
        if (!titularYaAsignado) {
            var ra = CorrespondenciaResponsable.builder()
                    .correspondencia(documento)
                    .usuario(titular)
                    .sumilla(req.observacion() != null ? req.observacion() : "")
                    .build();
            capturarFirma(ra, asignacionAsistente);
            documento.getResponsablesAsignados().add(ra);
            correspondenciaRepository.save(documento);
        }

        // ── 7. Historial ──
        var historial = CorrespondenciaHistorial.builder()
                .correspondencia(documento)
                .estadoAnterior(documento.getEstado())
                .estadoNuevo(documento.getEstado())
                .accion("RECEPCION_DELEGADA")
                .usuario(usuario)
                .detalle("Recepción delegada por " + usuario.getNombres() + " " + usuario.getApellidos()
                        + " (Secretaría) → Titular: " + titular.getNombres() + " " + titular.getApellidos()
                        + (req.observacion() != null && !req.observacion().isBlank()
                            ? " — " + req.observacion() : ""))
                .build();
        historialRepository.save(historial);

        // ── 8. Idempotencia ──
        var idem = CorrespondenciaRecepcionIdempotente.builder()
                .idempotencyKey(req.idempotencyKey())
                .correspondenciaId(req.correspondenciaId())
                .usuarioId(idUsuarioAsistente)
                .puestoTitularId(req.puestoTitularId())
                .build();
        idempotenteRepository.save(idem);

        // ── 9. Auditoría ──
        auditoriaEventos.registrar("RECEPCION_DELEGADA", "REGISTRO",
                "correspondencia", req.correspondenciaId(),
                null,
                java.util.Map.of(
                    "receptorReal", usuario.getUsername(),
                    "puestoTitular", req.puestoTitularId(),
                    "titular", titular.getUsername()
                ),
                "OK");

        // ── 10. Notificar al titular (post-commit via try/catch) ──
        try {
            notificacionService.notificarAsignacion(titular.getIdUsuario(), "CORRESPONDENCIA",
                    "Nuevo documento en su bandeja",
                    "Documento " + documento.getNumeroInterno() + " — " + documento.getAsunto()
                            + " (Recibido por Secretaría)",
                    documento.getIdCorrespondencia());
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación WebSocket al titular: {}", e.getMessage());
        }

        return documento;
    }

    /**
     * Resuelve el usuario activo que ocupa un puesto como responsable/jefe.
     * Busca usuarios con asignación vigente en ese puesto, priorizando jefatura.
     */
    private Usuario resolverUsuarioTitular(Integer idPuesto) {
        var usuarios = usuarioRepository.findByPuestoVigente(idPuesto);
        if (usuarios.isEmpty()) return null;
        // Preferir usuario cuya asignación vigente tenga puesto con esJefatura
        for (var u : usuarios) {
            if (u.getEmpleado() != null) {
                var asig = asignacionRepository
                        .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                                u.getEmpleado().getIdEmpleado(), "ACTIVA");
                if (asig.isPresent() && asig.get().getPuesto() != null
                        && Boolean.TRUE.equals(asig.get().getPuesto().getEsJefatura())) {
                    return u;
                }
            }
        }
        return usuarios.get(0);
    }

    private void capturarFirma(CorrespondenciaResponsable ra, AsignacionPuesto asignacion) {
        if (asignacion == null) return;
        if (asignacion.getPuesto() != null)
            ra.setPuestoFirmante(asignacion.getPuesto().getNombre());
        if (asignacion.getUnidadOrganizacional() != null)
            ra.setUnidadFirmante(asignacion.getUnidadOrganizacional().getNombre());
        ra.setAsignacionId(asignacion.getIdAsignacion());
    }
}
