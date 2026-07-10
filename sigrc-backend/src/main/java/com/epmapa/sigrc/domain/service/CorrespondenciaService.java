package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CorrespondenciaService {

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
                                  NotificacionWebSocketService notificacionService) {
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
                responsablesAsignados.add(CorrespondenciaResponsable.builder()
                    .correspondencia(null)
                    .usuario(u)
                    .sumilla(r.sumilla())
                    .build());
            }
        } else if ("SALIDA".equals(sentido)) {
            responsablesAsignados.add(CorrespondenciaResponsable.builder()
                .correspondencia(null)
                .usuario(creadoPor)
                .sumilla("")
                .build());
        }

        String numeroInterno = generarNumeroInterno();

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

        String accionHistorial = "INGRESO".equals(sentido) ? "CREACION" : "EMISION";
        String detalleHistorial = "INGRESO".equals(sentido)
                ? "Documento recibido y registrado en el sistema"
                : "Documento emitido y registrado en el sistema";
        registrarHistorial(entity, null, "RECIBIDO", accionHistorial, detalleHistorial, creadoPor);

        if (entity.getGeneraTicket()) {
            generarTicketDesdeCorrespondencia(entity, creadoPor);
        }

        var dto = toDTO(entity);
        for (var ra : responsablesAsignados) {
            notificacionService.notificarAsignacion(ra.getUsuario().getIdUsuario(), "CORRESPONDENCIA",
                "Correspondencia Asignada",
                "Documento " + entity.getNumeroInterno() + " - " + entity.getAsunto(),
                entity.getIdCorrespondencia());
        }
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
        }

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
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
        return generarTicketDesdeCorrespondencia(entity, usuario);
    }

    private TicketVinculadoDTO generarTicketDesdeCorrespondencia(Correspondencia entity, Usuario usuario) {
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
                null,
                null,
                null,
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

    private String generarNumeroInterno() {
        Integer correlativo = repository.maxCorrelativoAnioActual();
        if (correlativo == null) correlativo = 0;
        String anio = String.valueOf(java.time.Year.now().getValue());
        return "COR-" + anio + "-" + String.format("%05d", correlativo + 1);
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

    private void guardarDestinatarios(Correspondencia entity, List<CorrespondenciaDestinatarioDTO> destinatarios) {
        if (destinatarios == null) return;
        for (CorrespondenciaDestinatarioDTO dto : destinatarios) {
            CorrespondenciaDestinatario d = CorrespondenciaDestinatario.builder()
                    .correspondencia(entity)
                    .tipo(dto.tipo())
                    .idDestinatario(dto.idDestinatario())
                    .nombre(dto.nombre())
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
                        ra.getSumilla()))
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
                obtenerHistorial(entity.getIdCorrespondencia()),
                obtenerRespuestas(entity.getIdCorrespondencia()),
                obtenerTicketsVinculados(entity.getIdCorrespondencia()),
                referencias,
                destinatariosDTO
        );
    }

    private CorrespondenciaDestinatarioDTO toDestinatarioDTO(CorrespondenciaDestinatario d) {
        return new CorrespondenciaDestinatarioDTO(
                d.getIdCorrespondenciaDestinatario(),
                d.getTipo(),
                d.getIdDestinatario(),
                d.getNombre()
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
