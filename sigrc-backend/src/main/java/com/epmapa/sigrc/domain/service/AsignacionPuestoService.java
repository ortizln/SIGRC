package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.AsignacionDTO;
import com.epmapa.sigrc.domain.dto.AsignacionRequest;
import com.epmapa.sigrc.domain.dto.JefeInfoDTO;
import com.epmapa.sigrc.domain.entity.AsignacionPuesto;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AsignacionPuestoService {

    private static final String ACTIVA = "ACTIVA";
    private static final String FINALIZADA = "FINALIZADA";

    private final AsignacionPuestoRepository asignacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PuestoRepository puestoRepository;
    private final UnidadOrganizacionalRepository unidadRepository;
    private final AuditoriaEventos auditoriaEventos;

    public AsignacionPuestoService(AsignacionPuestoRepository asignacionRepository,
                                   EmpleadoRepository empleadoRepository,
                                   PuestoRepository puestoRepository,
                                   UnidadOrganizacionalRepository unidadRepository,
                                   AuditoriaEventos auditoriaEventos) {
        this.asignacionRepository = asignacionRepository;
        this.empleadoRepository = empleadoRepository;
        this.puestoRepository = puestoRepository;
        this.unidadRepository = unidadRepository;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional(readOnly = true)
    public List<AsignacionDTO> listarPorEmpleado(Integer idEmpleado) {
        return asignacionRepository.findByEmpleadoIdEmpleadoOrderByFechaInicioDesc(idEmpleado).stream()
            .map(AsignacionPuestoService::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public AsignacionDTO obtenerActual(Integer idEmpleado) {
        return asignacionRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(idEmpleado, ACTIVA)
            .map(AsignacionPuestoService::toDTO)
            .orElse(null);
    }

    @Transactional
    public AsignacionDTO asignar(AsignacionRequest req) {
        if (req.idEmpleado() == null || req.idPuesto() == null)
            throw new IllegalArgumentException("Empleado y puesto son obligatorios");

        var empleado = empleadoRepository.findById(req.idEmpleado())
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + req.idEmpleado()));
        var puesto = puestoRepository.findById(req.idPuesto())
            .orElseThrow(() -> new EntityNotFoundException("Puesto no encontrado: " + req.idPuesto()));
        if (!Boolean.TRUE.equals(puesto.getActivo()))
            throw new IllegalArgumentException("El puesto está inactivo y no puede ser asignado");

        var unidad = req.idUnidad() != null
            ? unidadRepository.findById(req.idUnidad())
                .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + req.idUnidad()))
            : puesto.getUnidadOrganizacional();
        if (unidad == null)
            throw new IllegalArgumentException("Debe indicar una unidad organizacional (o el puesto debe tener unidad)");
        if (!Boolean.TRUE.equals(unidad.getActivo()))
            throw new IllegalArgumentException("La unidad está inactiva y no puede recibir asignaciones");

        LocalDate fechaInicio = req.fechaInicio() != null ? req.fechaInicio() : LocalDate.now();

        // Cerrar asignación principal anterior (regla: no sobrescribir, conservar historial)
        var anterior = asignacionRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                empleado.getIdEmpleado(), ACTIVA);
        anterior.ifPresent(a -> {
            a.setEsPrincipal(false);
            a.setEstado(FINALIZADA);
            if (a.getFechaFin() == null)
                a.setFechaFin(fechaInicio.minusDays(1));
            asignacionRepository.save(a);
        });

        var nueva = AsignacionPuesto.builder()
            .empleado(empleado)
            .puesto(puesto)
            .unidadOrganizacional(unidad)
            .tipoAsignacion(req.tipoAsignacion() != null ? req.tipoAsignacion() : "TITULAR")
            .fechaInicio(fechaInicio)
            .fechaFin(req.fechaFin())
            .esPrincipal(true)
            .estado(ACTIVA)
            .observacion(req.observacion())
            .build();

        var creada = toDTO(asignacionRepository.save(nueva));
        auditoriaEventos.registrar("ASIGNAR_PUESTO", "REGISTRO", "asignacion_puesto", creada.idAsignacion(),
            null, Map.of("idEmpleado", req.idEmpleado(), "idPuesto", req.idPuesto(),
                "idUnidad", req.idUnidad(), "tipoAsignacion", req.tipoAsignacion()), "OK");
        return creada;
    }

    @Transactional
    public AsignacionDTO finalizar(Integer id) {
        var asignacion = asignacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asignación no encontrada: " + id));
        asignacion.setEstado(FINALIZADA);
        asignacion.setEsPrincipal(false);
        if (asignacion.getFechaFin() == null)
            asignacion.setFechaFin(LocalDate.now());
        var finalizada = toDTO(asignacionRepository.save(asignacion));
        auditoriaEventos.registrar("FINALIZAR_ASIGNACION", "MODIFICACION", "asignacion_puesto", id,
            null, Map.of("estado", FINALIZADA, "fechaFin", String.valueOf(asignacion.getFechaFin())), "OK");
        return finalizada;
    }

    /**
     * Determina el jefe inmediato de un empleado por estructura organizacional
     * (no por rol). Prioridad: responsable_asignacion_id de la unidad, luego
     * puesto marcado como responsable de unidad, luego puesto de jefatura.
     */
    @Transactional(readOnly = true)
    public JefeInfoDTO jefeInmediato(Integer idEmpleado) {
        var asignacionActual = asignacionRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(idEmpleado, ACTIVA)
            .orElse(null);
        if (asignacionActual == null || asignacionActual.getUnidadOrganizacional() == null)
            return null;

        var unidad = asignacionActual.getUnidadOrganizacional();

        // 1) Responsable explícito de la unidad (responsable_asignacion_id)
        if (unidad.getResponsableAsignacionId() != null) {
            var asigResponsable = asignacionRepository
                .findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueOrderByFechaInicioDesc(
                    unidad.getIdUnidad(), ACTIVA)
                .orElse(null);
            if (asigResponsable != null && asigResponsable.getIdAsignacion().equals(unidad.getResponsableAsignacionId()))
                return toJefeInfo(asigResponsable);
        }

        // 2) Empleado con puesto "responsable de unidad" en esa unidad
        var responsableUnidad = asignacionRepository
            .findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsResponsableUnidadTrueOrderByFechaInicioDesc(
                unidad.getIdUnidad(), ACTIVA)
            .orElse(null);
        if (responsableUnidad != null && !responsableUnidad.getIdAsignacion().equals(asignacionActual.getIdAsignacion()))
            return toJefeInfo(responsableUnidad);

        // 3) Empleado con puesto de jefatura en esa unidad
        var jefatura = asignacionRepository
            .findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsJefaturaTrueOrderByFechaInicioDesc(
                unidad.getIdUnidad(), ACTIVA)
            .orElse(null);
        if (jefatura != null && !jefatura.getIdAsignacion().equals(asignacionActual.getIdAsignacion()))
            return toJefeInfo(jefatura);

        // 4) Si la unidad tiene padre, el jefe es el responsable de la unidad padre
        if (unidad.getUnidadPadre() != null) {
            var padre = unidad.getUnidadPadre();
            var respPadre = resolverResponsable(padre.getIdUnidad());
            if (respPadre != null) return toJefeInfo(respPadre);
        }

        return null;
    }

    private AsignacionPuesto resolverResponsable(Integer idUnidad) {
        var unidad = unidadRepository.findById(idUnidad).orElse(null);
        if (unidad == null) return null;
        if (unidad.getResponsableAsignacionId() != null) {
            var asig = asignacionRepository
                .findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueOrderByFechaInicioDesc(
                    idUnidad, ACTIVA).orElse(null);
            if (asig != null && asig.getIdAsignacion().equals(unidad.getResponsableAsignacionId()))
                return asig;
        }
        return asignacionRepository
            .findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsResponsableUnidadTrueOrderByFechaInicioDesc(
                idUnidad, ACTIVA).orElse(null);
    }

    private static JefeInfoDTO toJefeInfo(AsignacionPuesto a) {
        var emp = a.getEmpleado();
        var puesto = a.getPuesto();
        var unidad = a.getUnidadOrganizacional();
        return new JefeInfoDTO(
            emp != null ? emp.getIdEmpleado() : null,
            emp != null ? (emp.getNombres() + " " + emp.getApellidos()) : null,
            puesto != null ? puesto.getIdPuesto() : null,
            puesto != null ? puesto.getNombre() : null,
            unidad != null ? unidad.getIdUnidad() : null,
            unidad != null ? unidad.getNombre() : null,
            a.getTipoAsignacion(),
            a.getFechaInicio() != null ? a.getFechaInicio().toString() : null
        );
    }

    private static AsignacionDTO toDTO(AsignacionPuesto a) {
        var emp = a.getEmpleado();
        var puesto = a.getPuesto();
        var unidad = a.getUnidadOrganizacional();
        return new AsignacionDTO(
            a.getIdAsignacion(),
            emp != null ? emp.getIdEmpleado() : null,
            emp != null ? (emp.getNombres() + " " + emp.getApellidos()) : null,
            puesto != null ? puesto.getIdPuesto() : null,
            puesto != null ? puesto.getCodigo() : null,
            puesto != null ? puesto.getNombre() : null,
            unidad != null ? unidad.getIdUnidad() : null,
            unidad != null ? unidad.getNombre() : null,
            a.getTipoAsignacion(),
            a.getFechaInicio(),
            a.getFechaFin(),
            a.getEsPrincipal(),
            a.getEstado(),
            a.getObservacion()
        );
    }
}
