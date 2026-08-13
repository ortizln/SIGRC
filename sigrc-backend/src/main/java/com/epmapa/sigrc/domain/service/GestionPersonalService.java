package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GestionPersonalService {

    public static final String ESTADO_BORRADOR = "BORRADOR";
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_PENDIENTE_JEFE = "PENDIENTE_JEFE";
    public static final String ESTADO_EN_REVISION = "EN_REVISION";
    public static final String ESTADO_APROBADA = "APROBADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";
    public static final String ESTADO_ANULADA = "ANULADA";

    private final MovimientoPersonalRepository movimientoRepository;
    private final AccionPersonalRepository accionRepository;
    private final SolicitudAusenciaRepository ausenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PuestoRepository puestoRepository;
    private final UnidadOrganizacionalRepository unidadRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionPuestoService asignacionService;
    private final AuditoriaEventos auditoriaEventos;

    public GestionPersonalService(MovimientoPersonalRepository movimientoRepository,
                                  AccionPersonalRepository accionRepository,
                                  SolicitudAusenciaRepository ausenciaRepository,
                                  EmpleadoRepository empleadoRepository,
                                  PuestoRepository puestoRepository,
                                  UnidadOrganizacionalRepository unidadRepository,
                                  AsignacionPuestoRepository asignacionRepository,
                                  UsuarioRepository usuarioRepository,
                                  AsignacionPuestoService asignacionService,
                                  AuditoriaEventos auditoriaEventos) {
        this.movimientoRepository = movimientoRepository;
        this.accionRepository = accionRepository;
        this.ausenciaRepository = ausenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.puestoRepository = puestoRepository;
        this.unidadRepository = unidadRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionService = asignacionService;
        this.auditoriaEventos = auditoriaEventos;
    }

    // ─────────────────── Movimientos de personal ───────────────────

    @Transactional(readOnly = true)
    public List<MovimientoPersonalDTO> listarMovimientos(Integer idEmpleado, String estado) {
        var lista = idEmpleado != null
            ? movimientoRepository.findByEmpleadoIdEmpleadoOrderByCreatedAtDesc(idEmpleado)
            : movimientoRepository.findAllByOrderByCreatedAtDesc();
        return lista.stream()
            .filter(m -> estado == null || estado.isBlank() || estado.equals(m.getEstado()))
            .map(this::toMovimientoDTO)
            .toList();
    }

    @Transactional
    public MovimientoPersonalDTO crearMovimiento(MovimientoPersonalRequest req, Integer idUsuario) {
        validarMovimiento(req);
        var empleado = empleadoRepository.findById(req.idEmpleado())
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + req.idEmpleado()));
        var creadoPor = usuarioRepository.getReferenceById(idUsuario);

        var movimiento = MovimientoPersonal.builder()
            .empleado(empleado)
            .tipoMovimiento(req.tipoMovimiento())
            .asignacionOrigen(req.idAsignacionOrigen() != null
                ? asignacionRepository.getReferenceById(req.idAsignacionOrigen()) : null)
            .puestoDestino(req.idPuestoDestino() != null
                ? puestoRepository.getReferenceById(req.idPuestoDestino()) : null)
            .unidadDestino(req.idUnidadDestino() != null
                ? unidadRepository.getReferenceById(req.idUnidadDestino()) : null)
            .fechaSolicitud(req.fechaSolicitud() != null ? req.fechaSolicitud() : LocalDate.now())
            .fechaDesde(req.fechaDesde())
            .fechaHasta(req.fechaHasta())
            .motivo(req.motivo())
            .documentoRespaldoId(req.documentoRespaldoId())
            .estado(ESTADO_BORRADOR)
            .creadoPor(creadoPor)
            .build();
        var creado = toMovimientoDTO(movimientoRepository.save(movimiento));
        auditoriaEventos.registrar("CREAR_MOVIMIENTO", "REGISTRO", "movimiento_personal",
            creado.idMovimiento(), null, resumenMovimiento(creado), "OK");
        return creado;
    }

    @Transactional
    public MovimientoPersonalDTO actualizarMovimiento(Integer id, MovimientoPersonalRequest req, Integer idUsuario) {
        var movimiento = obtenerMovimiento(id);
        if (!ESTADO_BORRADOR.equals(movimiento.getEstado()) && !ESTADO_RECHAZADA.equals(movimiento.getEstado()))
            throw new IllegalStateException("Solo se puede editar un movimiento en BORRADOR o RECHAZADO");
        if (req.idEmpleado() != null && !req.idEmpleado().equals(movimiento.getEmpleado().getIdEmpleado()))
            throw new IllegalStateException("No se puede cambiar el empleado del movimiento");
        if (req.tipoMovimiento() != null) movimiento.setTipoMovimiento(req.tipoMovimiento());
        if (req.idAsignacionOrigen() != null)
            movimiento.setAsignacionOrigen(asignacionRepository.getReferenceById(req.idAsignacionOrigen()));
        if (req.idPuestoDestino() != null)
            movimiento.setPuestoDestino(puestoRepository.getReferenceById(req.idPuestoDestino()));
        if (req.idUnidadDestino() != null)
            movimiento.setUnidadDestino(unidadRepository.getReferenceById(req.idUnidadDestino()));
        if (req.fechaSolicitud() != null) movimiento.setFechaSolicitud(req.fechaSolicitud());
        if (req.fechaDesde() != null) movimiento.setFechaDesde(req.fechaDesde());
        if (req.fechaHasta() != null) movimiento.setFechaHasta(req.fechaHasta());
        if (req.motivo() != null) movimiento.setMotivo(req.motivo());
        if (req.documentoRespaldoId() != null) movimiento.setDocumentoRespaldoId(req.documentoRespaldoId());
        if (ESTADO_RECHAZADA.equals(movimiento.getEstado())) movimiento.setEstado(ESTADO_BORRADOR);
        return toMovimientoDTO(movimientoRepository.save(movimiento));
    }

    @Transactional
    public MovimientoPersonalDTO enviarMovimiento(Integer id, Integer idUsuario) {
        var movimiento = obtenerMovimiento(id);
        if (!ESTADO_BORRADOR.equals(movimiento.getEstado()))
            throw new IllegalStateException("Solo se puede enviar un movimiento en BORRADOR");
        movimiento.setEstado(ESTADO_PENDIENTE);
        return toMovimientoDTO(movimientoRepository.save(movimiento));
    }

    @Transactional
    public MovimientoPersonalDTO aprobarMovimiento(Integer id, Integer idUsuario) {
        var movimiento = obtenerMovimiento(id);
        if (!ESTADO_PENDIENTE.equals(movimiento.getEstado()))
            throw new IllegalStateException("El movimiento no está pendiente de aprobación");
        var aprobador = usuarioRepository.getReferenceById(idUsuario);
        movimiento.setEstado(ESTADO_APROBADA);
        movimiento.setAprobadoPor(aprobador);
        var aprobado = toMovimientoDTO(movimientoRepository.save(movimiento));
        auditoriaEventos.registrar("APROBAR_MOVIMIENTO", "APROBACION", "movimiento_personal", id,
            null, Map.of("estado", ESTADO_APROBADA, "aprobadoPor", idUsuario), "OK");
        return aprobado;
    }

    @Transactional
    public MovimientoPersonalDTO rechazarMovimiento(Integer id, Integer idUsuario) {
        var movimiento = obtenerMovimiento(id);
        if (!ESTADO_PENDIENTE.equals(movimiento.getEstado()))
            throw new IllegalStateException("El movimiento no está pendiente de aprobación");
        movimiento.setEstado(ESTADO_RECHAZADA);
        return toMovimientoDTO(movimientoRepository.save(movimiento));
    }

    @Transactional
    public MovimientoPersonalDTO anularMovimiento(Integer id, Integer idUsuario) {
        var movimiento = obtenerMovimiento(id);
        if (ESTADO_APROBADA.equals(movimiento.getEstado()))
            throw new IllegalStateException("Un movimiento aprobado no puede anularse");
        movimiento.setEstado(ESTADO_ANULADA);
        return toMovimientoDTO(movimientoRepository.save(movimiento));
    }

    /**
     * Ejecutar el movimiento: registra la nueva asignación de puesto del empleado
     * (cierra la anterior y conserva historial). Aplica a TRASLADO, ENCARGO,
     * NOMBRAMIENTO, REINTEGRO, CAMBIO_ADMINISTRATIVO, TRASPASO y SUBROGACION.
     */
    @Transactional
    public MovimientoPersonalDTO ejecutarMovimiento(Integer id, Integer idUsuario) {
        var movimiento = obtenerMovimiento(id);
        if (!ESTADO_APROBADA.equals(movimiento.getEstado()))
            throw new IllegalStateException("Solo se puede ejecutar un movimiento aprobado");
        if (movimiento.getPuestoDestino() == null)
            throw new IllegalStateException("El movimiento debe indicar un puesto de destino");

        var req = new AsignacionRequest(
            movimiento.getEmpleado().getIdEmpleado(),
            movimiento.getPuestoDestino().getIdPuesto(),
            movimiento.getUnidadDestino() != null ? movimiento.getUnidadDestino().getIdUnidad() : null,
            tipoAsignacionDesdeMovimiento(movimiento.getTipoMovimiento()),
            movimiento.getFechaDesde(),
            movimiento.getFechaHasta(),
            "Ejecutado desde movimiento " + movimiento.getIdMovimiento()
        );
        asignacionService.asignar(req);
        return toMovimientoDTO(movimientoRepository.save(movimiento));
    }

    private String tipoAsignacionDesdeMovimiento(String tipoMovimiento) {
        if (tipoMovimiento == null) return "TITULAR";
        return switch (tipoMovimiento) {
            case "ENCARGO" -> "ENCARGO";
            case "SUBROGACION" -> "SUBROGACION";
            default -> "TITULAR";
        };
    }

    private void validarMovimiento(MovimientoPersonalRequest req) {
        if (req.idEmpleado() == null)
            throw new IllegalArgumentException("El empleado es obligatorio");
        if (req.tipoMovimiento() == null || req.tipoMovimiento().isBlank())
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        if (req.fechaDesde() != null && req.fechaHasta() != null
            && req.fechaHasta().isBefore(req.fechaDesde()))
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
    }

    private MovimientoPersonal obtenerMovimiento(Integer id) {
        return movimientoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Movimiento no encontrado: " + id));
    }

    // ─────────────────── Acciones de personal ───────────────────

    @Transactional(readOnly = true)
    public List<AccionPersonalDTO> listarAcciones(Integer idEmpleado, String estado) {
        var lista = idEmpleado != null
            ? accionRepository.findByEmpleadoIdEmpleadoOrderByCreatedAtDesc(idEmpleado)
            : accionRepository.findAllByOrderByCreatedAtDesc();
        return lista.stream()
            .filter(a -> estado == null || estado.isBlank() || estado.equals(a.getEstado()))
            .map(this::toAccionDTO)
            .toList();
    }

    @Transactional
    public AccionPersonalDTO crearAccion(AccionPersonalRequest req, Integer idUsuario) {
        if (req.idEmpleado() == null)
            throw new IllegalArgumentException("El empleado es obligatorio");
        if (req.tipo() == null || req.tipo().isBlank())
            throw new IllegalArgumentException("El tipo de acción de personal es obligatorio");
        var empleado = empleadoRepository.findById(req.idEmpleado())
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + req.idEmpleado()));
        var elaboradoPor = usuarioRepository.getReferenceById(idUsuario);

        var accion = AccionPersonal.builder()
            .numero(generarNumeroAccion())
            .empleado(empleado)
            .tipo(req.tipo())
            .fechaEmision(req.fechaEmision() != null ? req.fechaEmision() : LocalDate.now())
            .fechaVigenciaDesde(req.fechaVigenciaDesde())
            .fechaVigenciaHasta(req.fechaVigenciaHasta())
            .motivo(req.motivo())
            .situacionActual(req.situacionActual())
            .situacionPropuesta(req.situacionPropuesta())
            .documentoId(req.documentoId())
            .estado(ESTADO_BORRADOR)
            .elaboradoPor(elaboradoPor)
            .build();
        return toAccionDTO(accionRepository.save(accion));
    }

    @Transactional
    public AccionPersonalDTO actualizarAccion(Integer id, AccionPersonalRequest req, Integer idUsuario) {
        var accion = obtenerAccion(id);
        if (!ESTADO_BORRADOR.equals(accion.getEstado()) && !ESTADO_RECHAZADA.equals(accion.getEstado()))
            throw new IllegalStateException("Solo se puede editar una acción en BORRADOR o RECHAZADA");
        if (req.idEmpleado() != null && !req.idEmpleado().equals(accion.getEmpleado().getIdEmpleado()))
            throw new IllegalStateException("No se puede cambiar el empleado de la acción");
        if (req.tipo() != null) accion.setTipo(req.tipo());
        if (req.fechaEmision() != null) accion.setFechaEmision(req.fechaEmision());
        if (req.fechaVigenciaDesde() != null) accion.setFechaVigenciaDesde(req.fechaVigenciaDesde());
        if (req.fechaVigenciaHasta() != null) accion.setFechaVigenciaHasta(req.fechaVigenciaHasta());
        if (req.motivo() != null) accion.setMotivo(req.motivo());
        if (req.situacionActual() != null) accion.setSituacionActual(req.situacionActual());
        if (req.situacionPropuesta() != null) accion.setSituacionPropuesta(req.situacionPropuesta());
        if (req.documentoId() != null) accion.setDocumentoId(req.documentoId());
        if (ESTADO_RECHAZADA.equals(accion.getEstado())) accion.setEstado(ESTADO_BORRADOR);
        return toAccionDTO(accionRepository.save(accion));
    }

    @Transactional
    public AccionPersonalDTO enviarRevisionAccion(Integer id, Integer idUsuario) {
        var accion = obtenerAccion(id);
        if (!ESTADO_BORRADOR.equals(accion.getEstado()))
            throw new IllegalStateException("Solo se puede enviar a revisión una acción en BORRADOR");
        accion.setEstado(ESTADO_EN_REVISION);
        return toAccionDTO(accionRepository.save(accion));
    }

    @Transactional
    public AccionPersonalDTO aprobarAccion(Integer id, Integer idUsuario) {
        var accion = obtenerAccion(id);
        if (!ESTADO_EN_REVISION.equals(accion.getEstado()))
            throw new IllegalStateException("La acción debe estar en revisión para aprobarse");
        var aprobador = usuarioRepository.getReferenceById(idUsuario);
        accion.setEstado(ESTADO_APROBADA);
        accion.setAprobadoPor(aprobador);
        return toAccionDTO(accionRepository.save(accion));
    }

    @Transactional
    public AccionPersonalDTO rechazarAccion(Integer id, Integer idUsuario) {
        var accion = obtenerAccion(id);
        if (!ESTADO_EN_REVISION.equals(accion.getEstado()))
            throw new IllegalStateException("La acción debe estar en revisión para rechazarse");
        accion.setEstado(ESTADO_RECHAZADA);
        return toAccionDTO(accionRepository.save(accion));
    }

    @Transactional
    public AccionPersonalDTO anularAccion(Integer id, Integer idUsuario) {
        var accion = obtenerAccion(id);
        if (ESTADO_APROBADA.equals(accion.getEstado()))
            throw new IllegalStateException("Una acción aprobada no puede anularse");
        accion.setEstado(ESTADO_ANULADA);
        return toAccionDTO(accionRepository.save(accion));
    }

    private AccionPersonal obtenerAccion(Integer id) {
        return accionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Acción de personal no encontrada: " + id));
    }

    private String generarNumeroAccion() {
        String anio = String.valueOf(Year.now().getValue());
        long correlativo = accionRepository.count() + 1;
        return "AP-" + anio + "-" + String.format("%05d", correlativo);
    }

    // ─────────────────── Vacaciones, permisos y licencias ───────────────────

    @Transactional(readOnly = true)
    public List<SolicitudAusenciaDTO> listarAusencias(Integer idEmpleado, String estado) {
        var lista = idEmpleado != null
            ? ausenciaRepository.findByEmpleadoIdEmpleadoOrderByCreatedAtDesc(idEmpleado)
            : ausenciaRepository.findAllByOrderByCreatedAtDesc();
        return lista.stream()
            .filter(a -> estado == null || estado.isBlank() || estado.equals(a.getEstado()))
            .map(this::toAusenciaDTO)
            .toList();
    }

    /**
     * Crear solicitud de ausencia. Flujo: FUNCIONARIO → JEFE INMEDIATO → TALENTO HUMANO.
     * Estado inicial PENDIENTE_JEFE; el jefe se resuelve por estructura organizacional.
     */
    @Transactional
    public SolicitudAusenciaDTO crearAusencia(SolicitudAusenciaRequest req, Integer idUsuario) {
        validarAusencia(req);
        var empleado = empleadoRepository.findById(req.idEmpleado())
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + req.idEmpleado()));
        return toAusenciaDTO(ausenciaRepository.save(SolicitudAusencia.builder()
            .empleado(empleado)
            .tipo(req.tipo())
            .fechaDesde(req.fechaDesde())
            .fechaHasta(req.fechaHasta())
            .dias(req.dias())
            .horas(req.horas())
            .motivo(req.motivo())
            .documentoRespaldoId(req.documentoRespaldoId())
            .estado(ESTADO_PENDIENTE_JEFE)
            .build()));
    }

    /**
     * Aprobación del jefe inmediato. Verifica que el usuario autenticado es el jefe
     * del empleado solicitante según la estructura organizacional.
     */
    @Transactional
    public SolicitudAusenciaDTO aprobarAusenciaJefe(Integer id, Integer idUsuario) {
        var ausencia = obtenerAusencia(id);
        if (!ESTADO_PENDIENTE_JEFE.equals(ausencia.getEstado()))
            throw new IllegalStateException("La solicitud no está pendiente de aprobación del jefe");
        verificarJefe(ausencia, idUsuario);
        ausencia.setJefeAprobador(usuarioRepository.getReferenceById(idUsuario));
        ausencia.setEstado("PENDIENTE_TH");
        return toAusenciaDTO(ausenciaRepository.save(ausencia));
    }

    @Transactional
    public SolicitudAusenciaDTO aprobarAusenciaTh(Integer id, Integer idUsuario) {
        var ausencia = obtenerAusencia(id);
        if (!"PENDIENTE_TH".equals(ausencia.getEstado()))
            throw new IllegalStateException("La solicitud no está pendiente de aprobación de Talento Humano");
        ausencia.setThAprobador(usuarioRepository.getReferenceById(idUsuario));
        ausencia.setEstado(ESTADO_APROBADA);
        return toAusenciaDTO(ausenciaRepository.save(ausencia));
    }

    @Transactional
    public SolicitudAusenciaDTO rechazarAusencia(Integer id, Integer idUsuario) {
        var ausencia = obtenerAusencia(id);
        if (ESTADO_APROBADA.equals(ausencia.getEstado()))
            throw new IllegalStateException("Una solicitud aprobada no puede rechazarse");
        ausencia.setEstado(ESTADO_RECHAZADA);
        return toAusenciaDTO(ausenciaRepository.save(ausencia));
    }

    @Transactional
    public SolicitudAusenciaDTO anularAusencia(Integer id, Integer idUsuario) {
        var ausencia = obtenerAusencia(id);
        if (ESTADO_APROBADA.equals(ausencia.getEstado()))
            throw new IllegalStateException("Una solicitud aprobada no puede anularse");
        ausencia.setEstado(ESTADO_ANULADA);
        return toAusenciaDTO(ausenciaRepository.save(ausencia));
    }

    private void verificarJefe(SolicitudAusencia ausencia, Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        if (usuario.getEmpleado() == null)
            throw new IllegalStateException("El usuario no está vinculado a un empleado");
        var jefe = asignacionService.jefeInmediato(ausencia.getEmpleado().getIdEmpleado());
        if (jefe == null || jefe.idJefe() == null)
            throw new IllegalStateException("No se pudo determinar el jefe inmediato del solicitante");
        if (!jefe.idJefe().equals(usuario.getEmpleado().getIdEmpleado()))
            throw new IllegalStateException("Solo el jefe inmediato del solicitante puede aprobar esta solicitud");
    }

    private void validarAusencia(SolicitudAusenciaRequest req) {
        if (req.idEmpleado() == null)
            throw new IllegalArgumentException("El empleado es obligatorio");
        if (req.tipo() == null || req.tipo().isBlank())
            throw new IllegalArgumentException("El tipo de solicitud es obligatorio");
        if (req.fechaDesde() == null || req.fechaHasta() == null)
            throw new IllegalArgumentException("Las fechas de la solicitud son obligatorias");
        if (req.fechaHasta().isBefore(req.fechaDesde()))
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
        if (req.dias() != null && req.dias() <= 0)
            throw new IllegalArgumentException("Los días deben ser un número positivo");
    }

    private SolicitudAusencia obtenerAusencia(Integer id) {
        return ausenciaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Solicitud de ausencia no encontrada: " + id));
    }

    // ─── DTO mappers ───

    private Map<String, Object> resumenMovimiento(MovimientoPersonalDTO m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tipoMovimiento", m.tipoMovimiento());
        map.put("idEmpleado", m.idEmpleado());
        map.put("estado", m.estado());
        return map;
    }

    private MovimientoPersonalDTO toMovimientoDTO(MovimientoPersonal m) {
        var emp = m.getEmpleado();
        var origen = m.getAsignacionOrigen();
        var puesto = m.getPuestoDestino();
        var unidad = m.getUnidadDestino();
        return new MovimientoPersonalDTO(
            m.getIdMovimiento(),
            emp != null ? emp.getIdEmpleado() : null,
            emp != null ? (emp.getNombres() + " " + emp.getApellidos()) : null,
            m.getTipoMovimiento(),
            origen != null ? origen.getIdAsignacion() : null,
            origen != null && origen.getPuesto() != null ? origen.getPuesto().getNombre() : null,
            puesto != null ? puesto.getIdPuesto() : null,
            puesto != null ? puesto.getNombre() : null,
            unidad != null ? unidad.getIdUnidad() : null,
            unidad != null ? unidad.getNombre() : null,
            m.getFechaSolicitud(),
            m.getFechaDesde(),
            m.getFechaHasta(),
            m.getMotivo(),
            m.getDocumentoRespaldoId(),
            m.getEstado(),
            m.getCreadoPor() != null ? m.getCreadoPor().getIdUsuario() : null,
            m.getAprobadoPor() != null ? m.getAprobadoPor().getIdUsuario() : null
        );
    }

    private AccionPersonalDTO toAccionDTO(AccionPersonal a) {
        var emp = a.getEmpleado();
        return new AccionPersonalDTO(
            a.getIdAccion(),
            a.getNumero(),
            emp != null ? emp.getIdEmpleado() : null,
            emp != null ? (emp.getNombres() + " " + emp.getApellidos()) : null,
            a.getTipo(),
            a.getFechaEmision(),
            a.getFechaVigenciaDesde(),
            a.getFechaVigenciaHasta(),
            a.getMotivo(),
            a.getSituacionActual(),
            a.getSituacionPropuesta(),
            a.getDocumentoId(),
            a.getEstado(),
            a.getElaboradoPor() != null ? a.getElaboradoPor().getIdUsuario() : null,
            a.getRevisadoPor() != null ? a.getRevisadoPor().getIdUsuario() : null,
            a.getAprobadoPor() != null ? a.getAprobadoPor().getIdUsuario() : null
        );
    }

    private SolicitudAusenciaDTO toAusenciaDTO(SolicitudAusencia s) {
        var emp = s.getEmpleado();
        return new SolicitudAusenciaDTO(
            s.getIdSolicitud(),
            emp != null ? emp.getIdEmpleado() : null,
            emp != null ? (emp.getNombres() + " " + emp.getApellidos()) : null,
            s.getTipo(),
            s.getFechaDesde(),
            s.getFechaHasta(),
            s.getDias(),
            s.getHoras(),
            s.getMotivo(),
            s.getDocumentoRespaldoId(),
            s.getEstado(),
            s.getJefeAprobador() != null ? s.getJefeAprobador().getIdUsuario() : null,
            s.getThAprobador() != null ? s.getThAprobador().getIdUsuario() : null
        );
    }
}