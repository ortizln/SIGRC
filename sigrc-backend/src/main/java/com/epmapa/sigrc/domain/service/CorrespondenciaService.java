package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaCatRepository;
    private final TicketService ticketService;

    @Value("${app.upload.path:/data/sigrc/uploads}")
    private String uploadPath;

    public CorrespondenciaService(CorrespondenciaRepository repository,
                                  CorrespondenciaDocumentoTipoRepository tipoDocRepository,
                                  CorrespondenciaAdjuntoRepository adjuntoRepository,
                                  CorrespondenciaHistorialRepository historialRepository,
                                  CorrespondenciaRespuestaRepository respuestaRepository,
                                  CorrespondenciaTicketRepository ticketRepository,
                                  CorrespondenciaAreaRepository areaRepository,
                                  UsuarioRepository usuarioRepository,
                                  AreaRepository areaCatRepository,
                                  TicketService ticketService) {
        this.repository = repository;
        this.tipoDocRepository = tipoDocRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.historialRepository = historialRepository;
        this.respuestaRepository = respuestaRepository;
        this.ticketRepository = ticketRepository;
        this.areaRepository = areaRepository;
        this.usuarioRepository = usuarioRepository;
        this.areaCatRepository = areaCatRepository;
        this.ticketService = ticketService;
    }

    @Transactional
    public CorrespondenciaDTO crear(CorrespondenciaCrearRequest request, Integer idUsuario) {
        CorrespondenciaDocumentoTipo tipoDoc = tipoDocRepository.findById(request.idTipoDocumento())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de documento no encontrado"));
        Usuario creadoPor = usuarioRepository.getReferenceById(idUsuario);

        Usuario responsable = request.idResponsable() != null
                ? usuarioRepository.getReferenceById(request.idResponsable())
                : null;

        String numeroInterno = generarNumeroInterno();

        Correspondencia entity = Correspondencia.builder()
                .numeroInterno(numeroInterno)
                .codigoDocumento(request.codigoDocumento())
                .tipoDocumento(tipoDoc)
                .asunto(request.asunto())
                .resumenEjecutivo(request.resumenEjecutivo())
                .fechaDocumento(request.fechaDocumento())
                .fechaRecepcion(request.fechaRecepcion())
                .horaRecepcion(request.horaRecepcion())
                .personaEntrega(request.personaEntrega())
                .cargo(request.cargo())
                .institucion(request.institucion())
                .departamentoRemitente(request.departamentoRemitente())
                .responsable(responsable)
                .prioridad(request.prioridad() != null ? request.prioridad() : "MEDIA")
                .estado("RECIBIDO")
                .requiereRespuesta(request.requiereRespuesta() != null && request.requiereRespuesta())
                .fechaLimiteRespuesta(request.fechaLimiteRespuesta())
                .generaTicket(request.generaTicket() != null && request.generaTicket())
                .observaciones(request.observaciones())
                .creadoPor(creadoPor)
                .build();

        entity = repository.save(entity);

        guardarAreasEtiquetadas(entity, request.areasEtiquetadas());

        registrarHistorial(entity, null, "RECIBIDO", "CREACION",
                "Documento recibido y registrado en el sistema", creadoPor);

        if (entity.getGeneraTicket()) {
            generarTicketDesdeCorrespondencia(entity, creadoPor);
        }

        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public PaginacionDTO<CorrespondenciaDTO> listar(String texto, String estado, String prioridad,
                                            Integer idTipoDocumento, Integer idResponsable,
                                            LocalDate fechaDesde, LocalDate fechaHasta,
                                            int pagina, int tamanio) {
        Pageable pageable = PageRequest.of(pagina, tamanio);
        var page = repository.buscar(texto, estado, prioridad, idTipoDocumento, idResponsable,
                        fechaDesde, fechaHasta, pageable)
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
        if (request.idResponsable() != null)
            entity.setResponsable(usuarioRepository.getReferenceById(request.idResponsable()));
        if (request.prioridad() != null) entity.setPrioridad(request.prioridad());
        if (request.requiereRespuesta() != null) entity.setRequiereRespuesta(request.requiereRespuesta());
        if (request.fechaLimiteRespuesta() != null) entity.setFechaLimiteRespuesta(request.fechaLimiteRespuesta());
        if (request.generaTicket() != null) entity.setGeneraTicket(request.generaTicket());
        if (request.observaciones() != null) entity.setObservaciones(request.observaciones());

        entity = repository.save(entity);

        if (request.areasEtiquetadas() != null) {
            areaRepository.deleteByCorrespondenciaIdCorrespondencia(entity.getIdCorrespondencia());
            guardarAreasEtiquetadas(entity, request.areasEtiquetadas());
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
    public CorrespondenciaDTO asignarResponsable(Integer id, Integer idResponsable, Integer idUsuario) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        Usuario responsable = usuarioRepository.findById(idResponsable)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        entity.setResponsable(responsable);
        if ("RECIBIDO".equals(entity.getEstado())) {
            entity.setEstado("ASIGNADO");
        }
        entity = repository.save(entity);

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        registrarHistorial(entity, null, entity.getEstado(), "ASIGNACION",
                "Asignado a: " + responsable.getNombres(), usuario);

        return toDTO(entity);
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
                "Documento: " + entity.getNumeroInterno() + " - " + entity.getAsunto(),
                "Documento generado desde Correspondencia\n\nNúmero Interno: " + entity.getNumeroInterno()
                        + "\nAsunto: " + entity.getAsunto()
                        + "\nRemitente: " + entity.getPersonaEntrega()
                        + "\nFecha Recepción: " + entity.getFechaRecepcion(),
                null,
                null,
                "CORRESPONDENCIA"
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
    public void eliminar(Integer id) {
        Correspondencia entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Correspondencia no encontrada"));
        entity.setActivo(false);
        repository.save(entity);
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
                entity.getResponsable() != null ? entity.getResponsable().getIdUsuario() : null,
                entity.getResponsable() != null ? entity.getResponsable().getNombres() : null,
                entity.getPrioridad(),
                entity.getEstado(),
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
                obtenerTicketsVinculados(entity.getIdCorrespondencia())
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
