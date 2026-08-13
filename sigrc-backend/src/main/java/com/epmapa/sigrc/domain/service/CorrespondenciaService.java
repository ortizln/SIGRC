package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CorrespondenciaService {

    private static final Logger log = LoggerFactory.getLogger(CorrespondenciaService.class);
    private final CorrespondenciaRepository repository;
    private final CorrespondenciaDocumentoTipoRepository tipoDocRepository;
    private final CorrespondenciaAdjuntoRepository adjuntoRepository;
    private final CorrespondenciaHistorialRepository historialRepository;
    private final CorrespondenciaRespuestaRepository respuestaRepository;
    private final CorrespondenciaTicketRepository ticketRepository;
    private final CorrespondenciaAreaRepository areaRepository;
    private final CorrespondenciaDestinatarioRepository destinatarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaCatRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final AuditoriaService auditoriaService;
    private final TicketService ticketService;
    private final NotificacionWebSocketService notificacionService;
    private final AsignacionPuestoRepository asignacionPuestoRepository;
    private final UnidadOrganizacionalRepository unidadOrganizacionalRepository;
    private final AsignacionPuestoService asignacionPuestoService;
    private final DelegacionFuncionService delegacionService;

    @Value("${app.upload.path:/data/sigrc/uploads}")
    private String uploadPath;

    public CorrespondenciaService(CorrespondenciaRepository repository,
                                  CorrespondenciaDocumentoTipoRepository tipoDocRepository,
                                  CorrespondenciaAdjuntoRepository adjuntoRepository,
                                  CorrespondenciaHistorialRepository historialRepository,
                                  CorrespondenciaRespuestaRepository respuestaRepository,
                                  CorrespondenciaTicketRepository ticketRepository,
                                  CorrespondenciaAreaRepository areaRepository,
                                  CorrespondenciaDestinatarioRepository destinatarioRepository,
                                  UsuarioRepository usuarioRepository,
                                  AreaRepository areaCatRepository,
                                  UsuarioPermisoRepository usuarioPermisoRepository,
                                  AuditoriaService auditoriaService,
                                  TicketService ticketService,
                                  NotificacionWebSocketService notificacionService,
                                  AsignacionPuestoRepository asignacionPuestoRepository,
                                  UnidadOrganizacionalRepository unidadOrganizacionalRepository,
                                  AsignacionPuestoService asignacionPuestoService,
                                  DelegacionFuncionService delegacionService) {
        this.repository = repository;
        this.tipoDocRepository = tipoDocRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.historialRepository = historialRepository;
        this.respuestaRepository = respuestaRepository;
        this.ticketRepository = ticketRepository;
        this.areaRepository = areaRepository;
        this.destinatarioRepository = destinatarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.areaCatRepository = areaCatRepository;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
        this.auditoriaService = auditoriaService;
        this.ticketService = ticketService;
        this.notificacionService = notificacionService;
        this.asignacionPuestoRepository = asignacionPuestoRepository;
        this.unidadOrganizacionalRepository = unidadOrganizacionalRepository;
        this.asignacionPuestoService = asignacionPuestoService;
        this.delegacionService = delegacionService;
    }

    @Transactional
    public CorrespondenciaDTO crear(CorrespondenciaCrearRequest request, Integer idUsuario) {
        verificarPermisoModulo(idUsuario, "CORRESPONDENCIA", "ESCRITURA");
        CorrespondenciaDocumentoTipo tipoDoc = tipoDocRepository.findById(request.idTipoDocumento())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de documento no encontrado"));
        Usuario creadoPor = usuarioRepository.getReferenceById(idUsuario);

        String sentido = request.sentido() != null ? request.sentido() : "INGRESO";

        List<CorrespondenciaResponsable> responsablesAsignados = new ArrayList<>();
        if (request.responsables() != null && !request.responsables().isEmpty()) {
            for (var r : request.responsables()) {
                Usuario u = usuarioRepository.getReferenceById(r.idUsuario());
                var ra = CorrespondenciaResponsable.builder()
                    .correspondencia(null)
                    .usuario(u)
                    .sumilla(r.sumilla())
                    .build();
                capturarFirma(ra);
                responsablesAsignados.add(ra);
            }
        } else if ("SALIDA".equals(sentido)) {
            var ra = CorrespondenciaResponsable.builder()
                .correspondencia(null)
                .usuario(creadoPor)
                .sumilla("")
                .build();
            capturarFirma(ra);
            responsablesAsignados.add(ra);
        }

        String numeroInterno = generarNumeroInterno(creadoPor);

        if ("INGRESO".equals(sentido) && (request.personaEntrega() == null || request.personaEntrega().isBlank())) {
            throw new IllegalArgumentException("Persona que entrega es obligatoria para documentos de ingreso");
        }

        String personaEntrega = request.personaEntrega();
        if ("SALIDA".equals(sentido) && (personaEntrega == null || personaEntrega.isBlank())) {
            personaEntrega = "";
        }

        Correspondencia entity = Correspondencia.builder()
                .numeroInterno(numeroInterno)
                .codigoDocumento(request.codigoDocumento())
                .tipoDocumento(tipoDoc)
                .asunto(request.asunto())
                .resumenEjecutivo(request.resumenEjecutivo())
                .fechaDocumento(request.fechaDocumento())
                .fechaRecepcion(request.fechaRecepcion())
                .horaRecepcion(request.horaRecepcion())
                .personaEntrega(personaEntrega)
                .cargo(request.cargo())
                .institucion(request.institucion())
                .departamentoRemitente(request.departamentoRemitente())
                .prioridad(request.prioridad() != null ? request.prioridad() : "MEDIA")
                .estado("RECIBIDO")
                .sentido(sentido)
                .requiereRespuesta(request.requiereRespuesta() != null && request.requiereRespuesta())
                .fechaLimiteRespuesta(request.fechaLimiteRespuesta())
                .generaTicket(request.generaTicket() != null && request.generaTicket())
                .observaciones(request.observaciones())
                .creadoPor(creadoPor)
                .build();

        if (!responsablesAsignados.isEmpty()) {
            for (var ra : responsablesAsignados) {
                ra.setCorrespondencia(entity);
            }
            entity.getResponsablesAsignados().addAll(responsablesAsignados);
        }

        entity = repository.save(entity);

        guardarAreasEtiquetadas(entity, request.areasEtiquetadas());
        guardarReferencias(entity, request.idsReferencias());
        guardarDestinatarios(entity, request.destinatarios());
        marcarRespuestasDesdeReferencias(entity, creadoPor);

        if ("INGRESO".equals(sentido) && request.idRemitenteUsuario() != null) {
            Usuario remitente = usuarioRepository.findById(request.idRemitenteUsuario()).orElse(null);
            if (remitente != null) {
                Correspondencia salidaExistente = buscarSalidaRemitente(request.codigoDocumento(), remitente.getIdUsuario());
                if (salidaExistente != null) {
                    vincularMutuo(entity, salidaExistente);
                } else {
                    Correspondencia salidaEspejo = crearSalidaEspejo(request, remitente, creadoPor, entity);
                    vincularMutuo(entity, salidaEspejo);
                }
            }
        }

        String accionHistorial = "INGRESO".equals(sentido) ? "CREACION" : "EMISION";
        String detalleHistorial = "INGRESO".equals(sentido)
                ? "Documento recibido y registrado en el sistema"
                : "Documento emitido y registrado en el sistema";
        registrarHistorial(entity, null, "RECIBIDO", accionHistorial, detalleHistorial, creadoPor);

        if (entity.getGeneraTicket()) {
            try {
                generarTicketDesdeCorrespondencia(entity, creadoPor,
                    request.ticketIdSistema(), request.ticketIdCategoria(), request.ticketIdSubcategoria());
            } catch (Exception e) {
                log.warn("No se pudo generar el ticket automático: {}", e.getMessage());
            }
        }

        var dto = toDTO(entity);
        for (var ra : responsablesAsignados) {
            notificacionService.notificarAsignacion(ra.getUsuario().getIdUsuario(), "CORRESPONDENCIA",
                "Correspondencia Asignada",
                "Documento " + entity.getNumeroInterno() + " - " + entity.getAsunto(),
                entity.getIdCorrespondencia());
        }
        notificarDestinatarios(entity);
        return dto;
    }

    @Transactional(readOnly = true)
    public PaginacionDTO<CorrespondenciaDTO> listar(String texto, String estado, String prioridad,
                                            Integer idTipoDocumento, Integer idResponsable,
                                            Integer idUsuario,
                                            String sentido,
                                            LocalDate fechaDesde, LocalDate fechaHasta,
                                            int pagina, int tamanio,
                                            String sortBy, String sortDir) {
        String columna = sortBy != null ? sortBy : "creado_en";
        String direccion = sortDir != null && sortDir.equalsIgnoreCase("asc") ? "ASC" : "DESC";
        Sort sort = Sort.by(Sort.Direction.fromString(direccion), columna);
        Pageable pageable = PageRequest.of(pagina, tamanio, sort);
        var page = repository.buscar(texto, estado, prioridad, idTipoDocumento, idResponsable,
                        idUsuario, sentido, fechaDesde, fechaHasta, pageable)
                .map(this::toDTO);
        return new PaginacionDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Transactional(readOnly = true)
    public CorrespondenciaDTO obtener(Integer id) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        return toDTO(entity);
    }

    @Transactional
    public CorrespondenciaDTO actualizar(Integer id, CorrespondenciaActualizarRequest request, Integer idUsuario) {
        verificarPermisoModulo(idUsuario, "CORRESPONDENCIA", "ESCRITURA");
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));

        if (request.idTipoDocumento() != null)
            entity.setTipoDocumento(tipoDocRepository.getReferenceById(request.idTipoDocumento()));
        if (request.asunto() != null) entity.setAsunto(request.asunto());
        if (request.resumenEjecutivo() != null) entity.setResumenEjecutivo(request.resumenEjecutivo());
        if (request.codigoDocumento() != null) entity.setCodigoDocumento(request.codigoDocumento());
        if (request.fechaDocumento() != null) entity.setFechaDocumento(request.fechaDocumento());
        if (request.fechaRecepcion() != null) entity.setFechaRecepcion(request.fechaRecepcion());
        if (request.horaRecepcion() != null) entity.setHoraRecepcion(request.horaRecepcion());
        if (request.personaEntrega() != null) entity.setPersonaEntrega(request.personaEntrega());
        if (request.cargo() != null) entity.setCargo(request.cargo());
        if (request.institucion() != null) entity.setInstitucion(request.institucion());
        if (request.departamentoRemitente() != null) entity.setDepartamentoRemitente(request.departamentoRemitente());
        if (request.responsables() != null) {
            entity.getResponsablesAsignados().clear();
            if (!request.responsables().isEmpty()) {
                for (var r : request.responsables()) {
                    Usuario u = usuarioRepository.getReferenceById(r.idUsuario());
                    var ra = CorrespondenciaResponsable.builder()
                        .correspondencia(entity)
                        .usuario(u)
                        .sumilla(r.sumilla())
                        .build();
                    entity.getResponsablesAsignados().add(ra);
                }
            }
        }
        if (request.prioridad() != null) entity.setPrioridad(request.prioridad());
        if (request.requiereRespuesta() != null) entity.setRequiereRespuesta(request.requiereRespuesta());
        if (request.fechaLimiteRespuesta() != null) entity.setFechaLimiteRespuesta(request.fechaLimiteRespuesta());
        if (request.generaTicket() != null) entity.setGeneraTicket(request.generaTicket());
        if (request.observaciones() != null) entity.setObservaciones(request.observaciones());
        if (request.sentido() != null) entity.setSentido(request.sentido());

        entity = repository.save(entity);

        if (request.areasEtiquetadas() != null) {
            areaRepository.deleteByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia());
            guardarAreasEtiquetadas(entity, request.areasEtiquetadas());
        }
        if (request.idsReferencias() != null) {
            entity.getReferencias().clear();
            guardarReferencias(entity, request.idsReferencias());
            repository.save(entity);
        }
        if (request.destinatarios() != null) {
            destinatarioRepository.deleteByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia());
            guardarDestinatarios(entity, request.destinatarios());
            notificarDestinatarios(entity);
        }

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        if (request.idsReferencias() != null) {
            marcarRespuestasDesdeReferencias(entity, usuario);
        }
        registrarHistorial(entity, entity.getEstado(), entity.getEstado(), "ACTUALIZACION",
                "Documento actualizado", usuario);

        return toDTO(entity);
    }

    @Transactional
    public CorrespondenciaDTO cambiarEstado(Integer id, String estadoNuevo, String detalle, Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        String estadoAnterior = entity.getEstado();
        entity.setEstado(estadoNuevo);
        entity = repository.save(entity);

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        registrarHistorial(entity, estadoAnterior, estadoNuevo, "CAMBIO_ESTADO", detalle, usuario);

        return toDTO(entity);
    }

    @Transactional
    public CorrespondenciaDTO asignarResponsable(Integer id, Integer idResponsable, String sumilla, Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        Usuario responsable = usuarioRepository.findById(idResponsable)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        boolean yaAsignado = entity.getResponsablesAsignados().stream()
                .anyMatch(ra -> ra.getUsuario().getIdUsuario().equals(idResponsable));
        if (!yaAsignado) {
            var ra = CorrespondenciaResponsable.builder()
                .correspondencia(entity)
                .usuario(responsable)
                .sumilla(sumilla != null ? sumilla : "")
                .build();
            capturarFirma(ra);
            entity.getResponsablesAsignados().add(ra);
        }
        if ("RECIBIDO".equals(entity.getEstado())) {
            entity.setEstado("ASIGNADO");
        }
        entity = repository.save(entity);

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        String detalleHistorial = "Asignado a: " + responsable.getNombres();
        if (sumilla != null && !sumilla.isBlank()) {
            detalleHistorial += " — Sumilla: " + sumilla;
        }
        registrarHistorial(entity, null, entity.getEstado(), "ASIGNACION",
                detalleHistorial, usuario);

        var dto = toDTO(entity);
        notificacionService.notificarAsignacion(idResponsable, "CORRESPONDENCIA",
            "Correspondencia Asignada",
            "Documento " + entity.getNumeroInterno() + " - " + entity.getAsunto(),
            entity.getIdCorrespondencia());
        return dto;
    }

    @Transactional
    public CorrespondenciaRespuestaDTO registrarRespuesta(CorrespondenciaRespuestaRequest request, Integer idUsuario) {
        Correspondencia entity = repository.findById(request.idCorrespondencia())
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));

        CorrespondenciaDocumentoTipo tipoDoc = request.idTipoDocumento() != null
                ? tipoDocRepository.getReferenceById(request.idTipoDocumento())
                : null;
        Usuario responsable = request.idResponsable() != null
                ? usuarioRepository.getReferenceById(request.idResponsable())
                : null;

        CorrespondenciaRespuesta respuesta = CorrespondenciaRespuesta.builder()
                .correspondencia(entity)
                .fechaRespuesta(request.fechaRespuesta())
                .numeroDocumento(request.numeroDocumento())
                .tipoDocumento(tipoDoc)
                .responsable(responsable)
                .observaciones(request.observaciones())
                .build();
        respuesta = respuestaRepository.save(respuesta);

        if (!"ARCHIVADO".equals(entity.getEstado())) {
            entity.setEstado("RESPONDIDO");
            repository.save(entity);
        }

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        registrarHistorial(entity, entity.getEstado(), "RESPONDIDO", "RESPUESTA",
                "Respuesta registrada - Documento: " + (request.numeroDocumento() != null ? request.numeroDocumento() : "N/A"),
                usuario);

        return toRespuestaDTO(respuesta);
    }

    @Transactional
    public CorrespondenciaDTO marcarRecibido(Integer id, Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));

        boolean marcado = false;
        List<CorrespondenciaDestinatario> destinatarios =
                destinatarioRepository.findByCorrespondenciaIdCorrespondencia(id);
        for (var d : destinatarios) {
            if ("USUARIO".equals(d.getTipo()) && d.getIdDestinatario().equals(idUsuario)
                    && !Boolean.TRUE.equals(d.getRecibido())) {
                d.setRecibido(true);
                d.setFechaRecibido(LocalDateTime.now());
                destinatarioRepository.save(d);
                marcado = true;
            }
        }

        if (marcado) {
            Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
            registrarHistorial(entity, entity.getEstado(), entity.getEstado(), "RECIBIDO",
                "El destinatario " + usuario.getNombres() + " " + usuario.getApellidos()
                    + " marcó el documento como recibido", usuario);
        }

        return toDTO(entity);
    }

    @Transactional
    public CorrespondenciaDTO marcarLeido(Integer id, Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));

        for (var d : destinatarioRepository.findByCorrespondenciaIdCorrespondencia(id)) {
            if ("USUARIO".equals(d.getTipo()) && d.getIdDestinatario() != null
                    && d.getIdDestinatario().equals(idUsuario)
                    && !Boolean.TRUE.equals(d.getLeido())) {
                d.setLeido(true);
                d.setFechaLeido(LocalDateTime.now());
                destinatarioRepository.save(d);
            }
        }

        return toDTO(entity);
    }

    @Transactional
    public CorrespondenciaDTO recepcionarYDerivar(Integer id, String sumilla,
                                                    List<Integer> idsUsuariosDerivados, Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);

        // Marcar como recibido + guardar sumilla en el registro del destinatario actual
        for (var d : destinatarioRepository.findByCorrespondenciaIdCorrespondencia(id)) {
            if ("USUARIO".equals(d.getTipo()) && d.getIdDestinatario() != null
                    && d.getIdDestinatario().equals(idUsuario)) {
                if (!Boolean.TRUE.equals(d.getRecibido())) {
                    d.setRecibido(true);
                    d.setFechaRecibido(LocalDateTime.now());
                }
                if (sumilla != null && !sumilla.isBlank()) {
                    d.setSumilla(sumilla);
                }
                destinatarioRepository.save(d);
            }
        }

        // Etiquetar/derivar a otros usuarios (pasan a ser responsables con la sumilla)
        List<Integer> idsEtiquetas = new ArrayList<>();
        if (idsUsuariosDerivados != null) {
            for (Integer idDest : idsUsuariosDerivados) {
                if (idDest == null || idDest.equals(idUsuario)) continue;
                boolean yaAsignado = entity.getResponsablesAsignados().stream()
                        .anyMatch(ra -> ra.getUsuario() != null
                                && idDest.equals(ra.getUsuario().getIdUsuario()));
                if (!yaAsignado) {
                    Usuario u = usuarioRepository.getReferenceById(idDest);
                    var ra = CorrespondenciaResponsable.builder()
                            .correspondencia(entity)
                            .usuario(u)
                            .sumilla(sumilla != null ? sumilla : "")
                            .build();
                    capturarFirma(ra);
                    entity.getResponsablesAsignados().add(ra);
                    idsEtiquetas.add(idDest);
                }
            }
        }
        entity = repository.save(entity);

        String detalle = "Recibido por " + usuario.getNombres() + " " + usuario.getApellidos();
        if (sumilla != null && !sumilla.isBlank()) detalle += " — Sumilla: " + sumilla;
        if (!idsEtiquetas.isEmpty()) detalle += " — Derivado a: " + idsEtiquetas.size() + " usuario(s)";
        registrarHistorial(entity, entity.getEstado(), entity.getEstado(), "RECEPCION_DERIVACION", detalle, usuario);

        // Notificar a los usuarios derivados
        for (Integer idDest : idsEtiquetas) {
            notificacionService.notificarAsignacion(idDest, "CORRESPONDENCIA",
                "Documento Derivado para su atención",
                "Documento " + entity.getNumeroInterno() + (sumilla != null && !sumilla.isBlank() ? " — " + sumilla : ""),
                entity.getIdCorrespondencia());
        }

        return toDTO(entity);
    }

    /**
     * Derivación institucional: resuelve destinos por estructura organizacional
     * (USUARIO, PUESTO, UNIDAD, RESPONSABLE_UNIDAD, JEFE_INMEDIATO) y los deriva.
     */
    @Transactional
    public CorrespondenciaDTO derivarInstitucional(Integer id, String sumilla,
                                                   List<DestinoDerivacionDTO> destinos,
                                                   Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));

        Set<Integer> idsUsuarios = new LinkedHashSet<>();
        if (destinos != null) {
            for (var d : destinos) {
                resolverDestino(d, idUsuario, idsUsuarios);
            }
        }
        if (idsUsuarios.isEmpty()) {
            throw new IllegalArgumentException("No se pudo resolver ningún destinatario institucional");
        }
        return recepcionarYDerivar(id, sumilla, new ArrayList<>(idsUsuarios), idUsuario);
    }

    private void resolverDestino(DestinoDerivacionDTO destino, Integer idUsuario, Set<Integer> idsUsuarios) {
        if (destino == null) return;
        String tipo = destino.tipo();
        if (tipo == null) return;
        switch (tipo) {
            case "USUARIO" -> {
                agregarDestinatario(idsUsuarios, destino.idDestino());
            }
            case "PUESTO" -> usuarioRepository.findByPuestoVigente(destino.idDestino())
                    .forEach(u -> agregarDestinatario(idsUsuarios, u.getIdUsuario()));
            case "UNIDAD" -> usuarioRepository.findByUnidadVigente(destino.idDestino())
                    .forEach(u -> agregarDestinatario(idsUsuarios, u.getIdUsuario()));
            case "RESPONSABLE_UNIDAD" -> {
                var idResp = resolverResponsableUnidad(destino.idDestino());
                agregarDestinatario(idsUsuarios, idResp);
            }
            case "JEFE_INMEDIATO" -> {
                var idResp = resolverJefeInmediato(idUsuario);
                agregarDestinatario(idsUsuarios, idResp);
            }
            default -> throw new IllegalArgumentException("Tipo de destino no válido: " + tipo);
        }
    }

    /**
     * Añade un destinatario aplicando la delegación de funciones activa:
     * si el usuario destino tiene una delegación vigente, se resuelve al delegado.
     */
    private void agregarDestinatario(Set<Integer> idsUsuarios, Integer idUsuario) {
        if (idUsuario == null) return;
        Integer delegado = delegacionService.resolverDelegado(idUsuario);
        idsUsuarios.add(delegado != null ? delegado : idUsuario);
    }

    /**
     * Bandeja por unidad: documentos en los que participa la unidad organizacional
     * vigente del usuario autenticado (creador, responsable o destinatario).
     */
    @Transactional(readOnly = true)
    public List<CorrespondenciaDTO> bandejaUnidad(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getEmpleado() == null) return List.of();

        var asignacionActual = asignacionPuestoRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
            .orElse(null);
        if (asignacionActual == null || asignacionActual.getUnidadOrganizacional() == null) {
            return List.of();
        }
        Integer idUnidad = asignacionActual.getUnidadOrganizacional().getIdUnidad();

        // Incluir también las unidades hijas de la unidad del usuario
        var idsUnidades = unidadOrganizacionalRepository.findByIdWithHijas(idUnidad);
        var idsUsuarios = new LinkedHashSet<Integer>();
        for (Integer u : idsUnidades) {
            usuarioRepository.findByUnidadVigente(u).forEach(x -> idsUsuarios.add(x.getIdUsuario()));
        }
        if (idsUsuarios.isEmpty()) return List.of();

        return repository.findByUsuariosParticipanOrderByCreadoEnDesc(new ArrayList<>(idsUsuarios)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Bandeja por puesto: documentos en los que participan las personas que
     * ocupan el mismo puesto vigente del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public List<CorrespondenciaDTO> bandejaPuesto(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getEmpleado() == null) return List.of();

        var asignacionActual = asignacionPuestoRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
            .orElse(null);
        if (asignacionActual == null || asignacionActual.getPuesto() == null) {
            return List.of();
        }
        Integer idPuesto = asignacionActual.getPuesto().getIdPuesto();

        var idsUsuarios = new LinkedHashSet<Integer>();
        usuarioRepository.findByPuestoVigente(idPuesto).forEach(u -> idsUsuarios.add(u.getIdUsuario()));
        if (idsUsuarios.isEmpty()) return List.of();

        return repository.findByUsuariosParticipanOrderByCreadoEnDesc(new ArrayList<>(idsUsuarios)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Bandeja de pendientes: documentos abiertos (no respondidos/archivados)
     * en los que el usuario participa, ordenados por prioridad y plazo.
     */
    @Transactional(readOnly = true)
    public List<CorrespondenciaDTO> pendientes(Integer idUsuario) {
        return repository.findPendientesPorUsuario(idUsuario).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Integer resolverResponsableUnidad(Integer idUnidad) {
        if (idUnidad == null) return null;
        var unidad = unidadOrganizacionalRepository.findById(idUnidad).orElse(null);
        if (unidad == null) return null;

        // 1) Responsable explícito de la unidad (responsable_asignacion_id)
        if (unidad.getResponsableAsignacionId() != null) {
            var asig = asignacionPuestoRepository.findById(unidad.getResponsableAsignacionId()).orElse(null);
            if (asig != null && asig.getEmpleado() != null) {
                return usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(asig.getEmpleado().getIdEmpleado())
                        .map(Usuario::getIdUsuario).orElse(null);
            }
        }

        // 2) Empleado con puesto de jefatura en esa unidad
        return asignacionPuestoRepository
                .findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsJefaturaTrueOrderByFechaInicioDesc(
                        idUnidad, "ACTIVA")
                .filter(a -> a.getEmpleado() != null)
                .flatMap(a -> usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(a.getEmpleado().getIdEmpleado()))
                .map(Usuario::getIdUsuario)
                .orElse(null);
    }

    private Integer resolverJefeInmediato(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null || usuario.getEmpleado() == null) return null;
        var jefe = asignacionPuestoService.jefeInmediato(usuario.getEmpleado().getIdEmpleado());
        if (jefe == null || jefe.idJefe() == null) return null;
        return usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(jefe.idJefe())
                .map(Usuario::getIdUsuario)
                .orElse(null);
    }

    private void notificarDestinatarios(Correspondencia entity) {
        for (var d : destinatarioRepository.findByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia())) {
            if ("USUARIO".equals(d.getTipo())) {
                notificacionService.notificarAsignacion(d.getIdDestinatario(), "CORRESPONDENCIA",
                    "Nuevo Documento Asignado",
                    "Documento " + entity.getNumeroInterno() + " - " + entity.getAsunto(),
                    entity.getIdCorrespondencia());
            }
        }
    }

    @Transactional
    public CorrespondenciaAdjuntoDTO subirAdjunto(Integer idCorrespondencia, MultipartFile file,
                                                   String tipo, Integer idUsuario) throws IOException {
        Correspondencia entity = repository.findById(idCorrespondencia)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);

        String dir = uploadPath + "/correspondencia/" + idCorrespondencia + "/";
        Files.createDirectories(Paths.get(dir));

        String ext = "";
        String nombreOriginal = file.getOriginalFilename();
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            ext = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }
        String nombreArchivo = UUID.randomUUID().toString() + ext;

        Path rutaCompleta = Paths.get(dir, nombreArchivo);
        Files.copy(file.getInputStream(), rutaCompleta);

        String hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(file.getBytes());
            hash = HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            hash = UUID.randomUUID().toString();
        }

        CorrespondenciaAdjunto adjunto = CorrespondenciaAdjunto.builder()
                .correspondencia(entity)
                .tipo(tipo != null ? tipo : "ANEXO")
                .nombreOriginal(nombreOriginal != null ? nombreOriginal : "archivo")
                .nombreArchivo(nombreArchivo)
                .rutaArchivo(rutaCompleta.toString())
                .tipoMime(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .tamanoBytes(file.getSize())
                .hashSha256(hash)
                .usuario(usuario)
                .build();
        adjunto = adjuntoRepository.save(adjunto);

        return toAdjuntoDTO(adjunto);
    }

    @Transactional(readOnly = true)
    public List<CorrespondenciaAdjuntoDTO> listarAdjuntos(Integer idCorrespondencia) {
        return adjuntoRepository.findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(idCorrespondencia)
                .stream().map(this::toAdjuntoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Path getAdjuntoPath(Integer idAdjunto) {
        CorrespondenciaAdjunto adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new EntityNotFoundException("Adjunto no encontrado"));
        return Paths.get(adjunto.getRutaArchivo());
    }

    @Transactional(readOnly = true)
    public CorrespondenciaAdjuntoDTO obtenerAdjunto(Integer idAdjunto) {
        CorrespondenciaAdjunto adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new EntityNotFoundException("Adjunto no encontrado"));
        return toAdjuntoDTO(adjunto);
    }

    @Transactional
    public void eliminarAdjunto(Integer idAdjunto, Integer idUsuario) {
        CorrespondenciaAdjunto adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new EntityNotFoundException("Adjunto no encontrado"));
        try {
            Files.deleteIfExists(Paths.get(adjunto.getRutaArchivo()));
        } catch (IOException ignored) {}
        adjuntoRepository.delete(adjunto);
    }

    @Transactional(readOnly = true)
    public List<CorrespondenciaHistorialDTO> obtenerHistorial(Integer idCorrespondencia) {
        return historialRepository.findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnDesc(idCorrespondencia)
                .stream().map(this::toHistorialDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CorrespondenciaRespuestaDTO> obtenerRespuestas(Integer idCorrespondencia) {
        return respuestaRepository.findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(idCorrespondencia)
                .stream().map(this::toRespuestaDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketVinculadoDTO> obtenerTicketsVinculados(Integer idCorrespondencia) {
        return repository.findTicketsByCorrespondenciaId(idCorrespondencia).stream()
                .map(row -> new TicketVinculadoDTO(
                        (Integer) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3]))
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketVinculadoDTO generarTicketDesdeCorrespondencia(Integer idCorrespondencia, Integer idUsuario) {
        Correspondencia entity = repository.findById(idCorrespondencia)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        return generarTicketDesdeCorrespondencia(entity, usuario, null, null, null);
    }

    private TicketVinculadoDTO generarTicketDesdeCorrespondencia(Correspondencia entity, Usuario usuario,
                                                                  Integer idSistema, Integer idCategoria, Integer idSubcategoria) {
        Integer idArea = usuario.getArea() != null
                ? usuario.getArea().getIdArea()
                : areaCatRepository.findByActivoTrueOrderByNombre()
                    .stream().findFirst()
                    .map(Area::getIdArea)
                    .orElseThrow(() -> new IllegalStateException("No hay áreas disponibles para asignar el ticket"));

        var crearRequest = new com.epmapa.sigrc.domain.dto.TicketCrearRequest(
                "REQUERIMIENTO",
                entity.getPrioridad(),
                usuario.getIdUsuario(),
                idArea,
                idSistema,
                idCategoria,
                idSubcategoria,
                null,
                "Documento: " + entity.getNumeroInterno() + " - " + entity.getAsunto(),
                "Documento generado desde Correspondencia\n\nNúmero Interno: " + entity.getNumeroInterno()
                        + "\nAsunto: " + entity.getAsunto()
                        + "\nRemitente: " + entity.getPersonaEntrega()
                        + "\nFecha Recepción: " + entity.getFechaRecepcion(),
                null,
                null,
                "SISTEMA"
        );
        var ticketDTO = ticketService.crear(crearRequest);

        Ticket ticketRef = ticketService.getReferenceById(ticketDTO.idTicket());
        CorrespondenciaTicket ct = CorrespondenciaTicket.builder()
                .correspondencia(entity)
                .ticket(ticketRef)
                .build();
        ticketRepository.save(ct);

        entity.setGeneraTicket(true);
        repository.save(entity);

        return new TicketVinculadoDTO(
                ticketDTO.idTicket(),
                ticketDTO.numeroTicket(),
                ticketDTO.asunto(),
                ticketDTO.estado()
        );
    }

    @Transactional
    public TicketVinculadoDTO vincularTicketExistente(Integer idCorrespondencia, Integer idTicket, Integer idUsuario) {
        if (ticketRepository.existsByCorrespondenciaIdCorrespondenciaAndTicketIdTicket(idCorrespondencia, idTicket)) {
            throw new IllegalStateException("El ticket ya está vinculado a este documento");
        }
        Correspondencia entity = repository.getReferenceById(idCorrespondencia);
        Ticket ticket = ticketService.getReferenceById(idTicket);
        CorrespondenciaTicket ct = CorrespondenciaTicket.builder()
                .correspondencia(entity)
                .ticket(ticket)
                .build();
        ticketRepository.save(ct);
        return new TicketVinculadoDTO(idTicket, ticket.getNumeroTicket(), ticket.getAsunto(), ticket.getEstado());
    }

    @Transactional(readOnly = true)
    public CorrespondenciaDashboardDTO dashboard() {
        long total = repository.countActivos();
        long pendientes = repository.countActivos() - repository.count();
        long vencidos = repository.findVencidos().size();
        long conTicket = repository.countQueGeneraronTicket();
        double tiempoProm = repository.tiempoPromedioRespuestaHoras() != null
                ? repository.tiempoPromedioRespuestaHoras() : 0;

        List<CorrespondenciaDashboardDTO.ItemCount> porEstado = repository.countByEstado().stream()
                .map(row -> new CorrespondenciaDashboardDTO.ItemCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<CorrespondenciaDashboardDTO.ItemCount> porPrioridad = repository.countByPrioridad().stream()
                .map(row -> new CorrespondenciaDashboardDTO.ItemCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<CorrespondenciaDashboardDTO.ItemCount> porTipo = repository.countByTipoDocumento().stream()
                .map(row -> new CorrespondenciaDashboardDTO.ItemCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<CorrespondenciaDashboardDTO.ItemCount> porDepto = repository.countByDepartamentoRemitente().stream()
                .map(row -> new CorrespondenciaDashboardDTO.ItemCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        List<CorrespondenciaDashboardDTO.TendenciaMensual> tendencias = repository.tendenciasMensuales().stream()
                .map(row -> new CorrespondenciaDashboardDTO.TendenciaMensual((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        return new CorrespondenciaDashboardDTO(
                total, pendientes, 0L, vencidos, conTicket, tiempoProm,
                porEstado, porPrioridad, porTipo, porDepto, tendencias
        );
    }

    @Transactional
    public void eliminar(Integer id, Integer idUsuario, HttpServletRequest request) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        entity.setActivo(false);
        repository.save(entity);

        var usuario = usuarioRepository.findById(idUsuario).orElse(null);
        auditoriaService.registrar(
            usuario != null ? usuario.getUsername() : "desconocido",
            idUsuario, "Anulación de documento " + entity.getNumeroInterno(),
            "ANULACION", "correspondencia", id,
            entity, null, request, "EXITO"
        );
    }

    @Transactional(readOnly = true)
    public List<CorrespondenciaDocumentoTipo> listarTiposDocumento() {
        return tipoDocRepository.findByActivoTrueOrderByNombre();
    }

    // ─── Private helpers ───

    private String generarNumeroInterno(Usuario usuario) {
        String iniciales = obtenerIniciales(usuario);
        Integer correlativo = repository.maxCorrelativoPorPrefijo(iniciales);
        if (correlativo == null) correlativo = 0;
        String anio = String.valueOf(java.time.Year.now().getValue());
        return iniciales + "-" + anio + "-" + String.format("%07d", correlativo + 1);
    }

    private String obtenerIniciales(Usuario usuario) {
        String nombre = usuario.getNombres() != null ? usuario.getNombres() : "";
        String apellido = usuario.getApellidos() != null ? usuario.getApellidos() : "";
        String iniNombre = nombre.isEmpty() ? "" : String.valueOf(nombre.charAt(0)).toUpperCase();
        String iniApellido = apellido.isEmpty() ? "" : String.valueOf(apellido.charAt(0)).toUpperCase();
        return iniNombre + iniApellido;
    }

    private void guardarAreasEtiquetadas(Correspondencia entity, List<Integer> areaIds) {
        if (areaIds == null) return;
        for (Integer idArea : areaIds) {
            Area area = areaCatRepository.getReferenceById(idArea);
            CorrespondenciaArea ca = CorrespondenciaArea.builder()
                    .correspondencia(entity)
                    .area(area)
                    .build();
            areaRepository.save(ca);
        }
    }

    private void guardarReferencias(Correspondencia entity, List<Integer> idsReferencias) {
        if (idsReferencias == null) return;
        entity.getReferencias().clear();
        for (Integer idRef : idsReferencias) {
            Correspondencia ref = repository.getReferenceById(idRef);
            entity.getReferencias().add(ref);
        }
    }

    private Correspondencia buscarSalidaRemitente(String codigoDocumento, Integer idRemitente) {
        if (codigoDocumento == null || codigoDocumento.isBlank()) return null;
        return repository.findByCodigoDocumentoAndSentido(codigoDocumento, "SALIDA").stream()
            .filter(c -> {
                if (c.getCreadoPor() != null && c.getCreadoPor().getIdUsuario().equals(idRemitente)) return true;
                return c.getResponsablesAsignados() != null && c.getResponsablesAsignados().stream()
                    .anyMatch(ra -> ra.getUsuario() != null && ra.getUsuario().getIdUsuario().equals(idRemitente));
            })
            .findFirst().orElse(null);
    }

    private void vincularMutuo(Correspondencia ingreso, Correspondencia salida) {
        if (!salida.getReferencias().contains(ingreso)) {
            salida.getReferencias().add(ingreso);
            repository.save(salida);
        }
        if (!ingreso.getReferencias().contains(salida)) {
            ingreso.getReferencias().add(salida);
            repository.save(ingreso);
        }
    }

    private Correspondencia crearSalidaEspejo(CorrespondenciaCrearRequest request, Usuario remitente,
                                              Usuario activoUsuario, Correspondencia ingreso) {
        CorrespondenciaDocumentoTipo tipoDoc = tipoDocRepository.findById(request.idTipoDocumento())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de documento no encontrado"));

        String numeroInterno = generarNumeroInterno(remitente);

        Correspondencia salida = Correspondencia.builder()
                .numeroInterno(numeroInterno)
                .codigoDocumento(request.codigoDocumento())
                .tipoDocumento(tipoDoc)
                .asunto(request.asunto())
                .resumenEjecutivo(request.resumenEjecutivo())
                .fechaDocumento(request.fechaDocumento())
                .fechaRecepcion(request.fechaRecepcion())
                .horaRecepcion(request.horaRecepcion())
                .personaEntrega(request.personaEntrega() != null ? request.personaEntrega() : "")
                .cargo(request.cargo())
                .institucion(request.institucion())
                .departamentoRemitente(request.departamentoRemitente())
                .prioridad(request.prioridad() != null ? request.prioridad() : "MEDIA")
                .estado("RECIBIDO")
                .sentido("SALIDA")
                .requiereRespuesta(false)
                .fechaLimiteRespuesta(request.fechaLimiteRespuesta())
                .generaTicket(false)
                .observaciones("Documento espejo generado automáticamente a partir del ingreso registrado por " + activoUsuario.getNombres())
                .creadoPor(remitente)
                .build();

        CorrespondenciaResponsable ra = CorrespondenciaResponsable.builder()
                .correspondencia(salida)
                .usuario(remitente)
                .sumilla("")
                .build();
        salida.getResponsablesAsignados().add(ra);

        salida = repository.save(salida);

        CorrespondenciaDestinatario d = CorrespondenciaDestinatario.builder()
                .correspondencia(salida)
                .tipo("USUARIO")
                .idDestinatario(activoUsuario.getIdUsuario())
                .nombre(activoUsuario.getNombres() + " " + (activoUsuario.getApellidos() != null ? activoUsuario.getApellidos() : ""))
                .recibido(false)
                .build();
        destinatarioRepository.save(d);

        registrarHistorial(salida, null, "RECIBIDO", "EMISION",
                "Documento espejo generado automáticamente a partir del ingreso " + ingreso.getNumeroInterno(), remitente);

        return salida;
    }

    private void marcarRespuestasDesdeReferencias(Correspondencia entity, Usuario usuario) {
        if (entity.getReferencias() == null || entity.getReferencias().isEmpty()) return;
        for (Correspondencia ref : entity.getReferencias()) {
            String numeroRespuesta = entity.getCodigoDocumento() != null
                    ? entity.getCodigoDocumento() : entity.getNumeroInterno();
            boolean yaExiste = respuestaRepository
                    .findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(ref.getIdCorrespondencia())
                    .stream().anyMatch(r -> numeroRespuesta != null && numeroRespuesta.equals(r.getNumeroDocumento()));
            if (yaExiste) continue;

            CorrespondenciaRespuesta respuesta = CorrespondenciaRespuesta.builder()
                    .correspondencia(ref)
                    .fechaRespuesta(LocalDate.now())
                    .numeroDocumento(numeroRespuesta)
                    .tipoDocumento(entity.getTipoDocumento())
                    .responsable(usuario)
                    .observaciones("Respondido mediante documento emitido " + entity.getNumeroInterno())
                    .build();
            respuestaRepository.save(respuesta);

            String estadoAnterior = ref.getEstado();
            if (!"ARCHIVADO".equals(estadoAnterior)) {
                ref.setEstado("RESPONDIDO");
                repository.save(ref);
            }
            registrarHistorial(ref, estadoAnterior, "RESPONDIDO", "RESPUESTA",
                    "Respuesta registrada mediante documento " + entity.getNumeroInterno(), usuario);
        }
    }

    private void guardarDestinatarios(Correspondencia entity, List<CorrespondenciaDestinatarioDTO> destinatarios) {
        if (destinatarios == null) return;
        for (CorrespondenciaDestinatarioDTO dto : destinatarios) {
            CorrespondenciaDestinatario d = CorrespondenciaDestinatario.builder()
                    .correspondencia(entity)
                    .tipo(dto.tipo())
                    .idDestinatario(dto.idDestinatario())
                    .nombre(dto.nombre())
                    .recibido(false)
                    .sumilla(dto.sumilla())
                    .build();
            destinatarioRepository.save(d);
        }
    }

    private void verificarPermisoModulo(Integer idUsuario, String modulo, String tipoAcceso) {
        var usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        if ("ADMIN".equals(usuario.getRol().getCodigo())) return;
        var permiso = usuarioPermisoRepository.findByUsuarioIdUsuarioAndModuloAndActivoTrue(idUsuario, modulo);
        if (permiso.isEmpty()) return;
        String acceso = permiso.get().getTipoAcceso();
        if ("LECTURA".equals(acceso) && "ESCRITURA".equals(tipoAcceso)) {
            throw new SecurityException("No tiene permisos de escritura en el módulo " + modulo);
        }
    }

    /**
     * Captura la instantánea institucional del firmante (puesto/unidad/asignación vigente)
     * en el momento de la sumilla, para conservar historial aunque cambie de puesto.
     */
    private void capturarFirma(CorrespondenciaResponsable ra) {
        if (ra.getUsuario() == null || ra.getUsuario().getEmpleado() == null) return;
        asignacionPuestoRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                ra.getUsuario().getEmpleado().getIdEmpleado(), "ACTIVA")
            .ifPresent(asig -> {
                ra.setAsignacionId(asig.getIdAsignacion());
                if (asig.getPuesto() != null) ra.setPuestoFirmante(asig.getPuesto().getNombre());
                if (asig.getUnidadOrganizacional() != null) {
                    ra.setUnidadFirmante(asig.getUnidadOrganizacional().getNombre());
                }
            });
    }

    private void registrarHistorial(Correspondencia entity, String estadoAnterior,
                                     String estadoNuevo, String accion, String detalle, Usuario usuario) {
        CorrespondenciaHistorial h = CorrespondenciaHistorial.builder()
                .correspondencia(entity)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .accion(accion)
                .usuario(usuario)
                .detalle(detalle)
                .build();
        historialRepository.save(h);
    }

    private CorrespondenciaDTO toDTO(Correspondencia entity) {
        List<Integer> areaIds = areaRepository.findByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia())
                .stream().map(ca -> ca.getArea().getIdArea()).collect(Collectors.toList());
        List<String> areaNombres = areaRepository.findByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia())
                .stream().map(ca -> ca.getArea().getNombre()).collect(Collectors.toList());

        List<CorrespondenciaReferenciaDTO> referencias = entity.getReferencias().stream()
                .map(ref -> new CorrespondenciaReferenciaDTO(
                        ref.getIdCorrespondencia(),
                        ref.getNumeroInterno(),
                        ref.getAsunto(),
                        ref.getCodigoDocumento()))
                .collect(Collectors.toList());

        List<CorrespondenciaReferenciaDTO> referenciadoPor = repository.findReferenciadoPor(entity.getIdCorrespondencia()).stream()
                .map(ref -> new CorrespondenciaReferenciaDTO(
                        ref.getIdCorrespondencia(),
                        ref.getNumeroInterno(),
                        ref.getAsunto(),
                        ref.getCodigoDocumento()))
                .collect(Collectors.toList());

        List<CorrespondenciaDestinatarioDTO> destinatariosDTO = destinatarioRepository
                .findByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia())
                .stream().map(this::toDestinatarioDTO).collect(Collectors.toList());

        return new CorrespondenciaDTO(
                entity.getIdCorrespondencia(),
                entity.getNumeroInterno(),
                entity.getCodigoDocumento(),
                entity.getTipoDocumento().getIdTipoDocumento(),
                entity.getTipoDocumento().getNombre(),
                entity.getTipoDocumento().getCodigo(),
                entity.getAsunto(),
                entity.getResumenEjecutivo(),
                entity.getFechaDocumento(),
                entity.getFechaRecepcion(),
                entity.getHoraRecepcion(),
                entity.getPersonaEntrega(),
                entity.getCargo(),
                entity.getInstitucion(),
                entity.getDepartamentoRemitente(),
                entity.getResponsablesAsignados().stream()
                    .map(ra -> new ResponsableAsignadoDTO(
                        ra.getUsuario().getIdUsuario(),
                        ra.getUsuario().getNombres() + " " + ra.getUsuario().getApellidos(),
                        ra.getSumilla(),
                        ra.getPuestoFirmante(),
                        ra.getUnidadFirmante()))
                    .collect(Collectors.toList()),
                entity.getPrioridad(),
                entity.getEstado(),
                entity.getSentido(),
                entity.getRequiereRespuesta(),
                entity.getFechaLimiteRespuesta(),
                entity.getGeneraTicket(),
                entity.getObservaciones(),
                entity.getActivo(),
                entity.getCreadoEn(),
                entity.getCreadoPor().getIdUsuario(),
                entity.getCreadoPor().getNombres(),
                areaIds,
                areaNombres,
                listarAdjuntos(entity.getIdCorrespondencia()),
                (int) adjuntoRepository.countByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia()),
                obtenerHistorial(entity.getIdCorrespondencia()),
                obtenerRespuestas(entity.getIdCorrespondencia()),
                obtenerTicketsVinculados(entity.getIdCorrespondencia()),
                referencias,
                referenciadoPor,
                destinatariosDTO
        );
    }

    private CorrespondenciaDestinatarioDTO toDestinatarioDTO(CorrespondenciaDestinatario d) {
        return new CorrespondenciaDestinatarioDTO(
                d.getIdCorrespondenciaDestinatario(),
                d.getTipo(),
                d.getIdDestinatario(),
                d.getNombre(),
                d.getRecibido(),
                d.getFechaRecibido(),
                d.getLeido(),
                d.getFechaLeido(),
                d.getSumilla()
        );
    }

    private CorrespondenciaAdjuntoDTO toAdjuntoDTO(CorrespondenciaAdjunto a) {
        return new CorrespondenciaAdjuntoDTO(
                a.getIdAdjunto(),
                a.getCorrespondencia().getIdCorrespondencia(),
                a.getTipo(),
                a.getNombreOriginal(),
                a.getNombreArchivo(),
                a.getTipoMime(),
                a.getTamanoBytes(),
                a.getHashSha256(),
                a.getUsuario().getIdUsuario(),
                a.getUsuario().getNombres(),
                a.getCreadoEn()
        );
    }

    private CorrespondenciaHistorialDTO toHistorialDTO(CorrespondenciaHistorial h) {
        return new CorrespondenciaHistorialDTO(
                h.getIdHistorial(),
                h.getCorrespondencia().getIdCorrespondencia(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getAccion(),
                h.getUsuario().getIdUsuario(),
                h.getUsuario().getNombres(),
                h.getDetalle(),
                h.getCreadoEn()
        );
    }

    private CorrespondenciaRespuestaDTO toRespuestaDTO(CorrespondenciaRespuesta r) {
        return new CorrespondenciaRespuestaDTO(
                r.getIdRespuesta(),
                r.getCorrespondencia().getIdCorrespondencia(),
                r.getFechaRespuesta(),
                r.getNumeroDocumento(),
                r.getTipoDocumento() != null ? r.getTipoDocumento().getIdTipoDocumento() : null,
                r.getTipoDocumento() != null ? r.getTipoDocumento().getNombre() : null,
                r.getResponsable() != null ? r.getResponsable().getIdUsuario() : null,
                r.getResponsable() != null ? r.getResponsable().getNombres() : null,
                r.getObservaciones(),
                r.getCreadoEn()
        );
    }
}
