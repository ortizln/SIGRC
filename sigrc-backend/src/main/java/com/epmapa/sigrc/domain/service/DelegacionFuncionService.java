package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.DelegacionFuncionDTO;
import com.epmapa.sigrc.domain.dto.DelegacionFuncionRequest;
import com.epmapa.sigrc.domain.dto.DelegacionResueltaDTO;
import com.epmapa.sigrc.domain.entity.DelegacionFuncion;
import com.epmapa.sigrc.domain.entity.Empleado;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.DelegacionFuncionRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DelegacionFuncionService {

    public static final String ACTIVA = "ACTIVA";
    public static final String CANCELADA = "CANCELADA";
    public static final String FINALIZADA = "FINALIZADA";

    private final DelegacionFuncionRepository delegacionRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;

    public DelegacionFuncionService(DelegacionFuncionRepository delegacionRepository,
                                    AsignacionPuestoRepository asignacionRepository,
                                    UsuarioRepository usuarioRepository) {
        this.delegacionRepository = delegacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DelegacionFuncionDTO> listar() {
        return delegacionRepository.findAllByOrderByFechaInicioDesc().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public DelegacionFuncionDTO crear(DelegacionFuncionRequest req, Integer idUsuario) {
        if (req.idAsignacionOrigen() == null || req.idAsignacionDelegada() == null)
            throw new IllegalArgumentException("Debe indicar la asignación de origen y la asignación delegada");
        if (req.fechaInicio() == null)
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");

        var origen = asignacionRepository.findById(req.idAsignacionOrigen())
            .orElseThrow(() -> new EntityNotFoundException("Asignación de origen no encontrada"));
        var delegada = asignacionRepository.findById(req.idAsignacionDelegada())
            .orElseThrow(() -> new EntityNotFoundException("Asignación delegada no encontrada"));
        if (origen.getIdAsignacion().equals(delegada.getIdAsignacion()))
            throw new IllegalArgumentException("La asignación delegada debe ser distinta de la de origen");

        LocalDate fin = req.fechaFin() != null ? req.fechaFin() : LocalDate.MAX;
        var solapadas = delegacionRepository
            .findByAsignacionOrigenIdAsignacionAndEstadoOrderByFechaInicioDesc(origen.getIdAsignacion(), ACTIVA);
        for (var d : solapadas) {
            LocalDate dIni = d.getFechaInicio();
            LocalDate dFin = d.getFechaFin() != null ? d.getFechaFin() : LocalDate.MAX;
            if (!dIni.isAfter(fin) && !dFin.isBefore(req.fechaInicio()))
                throw new IllegalArgumentException("Ya existe una delegación ACTIVA para este funcionario en el período indicado");
        }

        var delegacion = DelegacionFuncion.builder()
            .asignacionOrigen(origen)
            .asignacionDelegada(delegada)
            .fechaInicio(req.fechaInicio())
            .fechaFin(req.fechaFin())
            .tipo(req.tipo())
            .alcance(req.alcance())
            .documentoRespaldoId(req.documentoRespaldoId())
            .observacion(req.observacion())
            .estado(ACTIVA)
            .creadoPor(idUsuario)
            .build();
        return toDTO(delegacionRepository.save(delegacion));
    }

    @Transactional
    public DelegacionFuncionDTO cancelar(Integer id) {
        var d = obtener(id);
        if (FINALIZADA.equals(d.getEstado()))
            throw new IllegalArgumentException("La delegación ya finalizó");
        d.setEstado(CANCELADA);
        return toDTO(delegacionRepository.save(d));
    }

    @Transactional
    public DelegacionFuncionDTO finalizar(Integer id) {
        var d = obtener(id);
        d.setEstado(FINALIZADA);
        return toDTO(delegacionRepository.save(d));
    }

    private DelegacionFuncion obtener(Integer id) {
        return delegacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Delegación no encontrada: " + id));
    }

    /**
     * Si el usuario tiene una delegación ACTIVA vigente para hoy sobre su
     * asignación principal actual, devuelve el id del usuario delegado;
     * en caso contrario devuelve null.
     */
    @Transactional(readOnly = true)
    public Integer resolverDelegado(Integer idUsuario) {
        if (idUsuario == null) return null;
        var usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null || usuario.getEmpleado() == null) return null;

        var asignacion = asignacionRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
            .orElse(null);
        if (asignacion == null) return null;

        LocalDate hoy = LocalDate.now();
        return delegacionRepository
            .findByAsignacionOrigenIdAsignacionAndEstadoOrderByFechaInicioDesc(asignacion.getIdAsignacion(), ACTIVA)
            .stream()
            .filter(d -> !d.getFechaInicio().isAfter(hoy)
                && (d.getFechaFin() == null || !d.getFechaFin().isBefore(hoy)))
            .max(Comparator.comparing(DelegacionFuncion::getFechaInicio))
            .map(d -> {
                var emp = d.getAsignacionDelegada().getEmpleado();
                if (emp == null) return null;
                return usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(emp.getIdEmpleado())
                    .map(Usuario::getIdUsuario)
                    .orElse(null);
            })
            .orElse(null);
    }

    /**
     * Resuelve la delegación activa del usuario y retorna toda la información
     * necesaria para registrar la proveniencia en historial y destinatarios.
     */
    @Transactional(readOnly = true)
    public DelegacionResueltaDTO resolverDelegadoConDetalle(Integer idUsuario) {
        if (idUsuario == null) return null;
        var usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null || usuario.getEmpleado() == null) return null;

        var asignacion = asignacionRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
            .orElse(null);
        if (asignacion == null) return null;

        LocalDate hoy = LocalDate.now();
        return delegacionRepository
            .findByAsignacionOrigenIdAsignacionAndEstadoOrderByFechaInicioDesc(asignacion.getIdAsignacion(), ACTIVA)
            .stream()
            .filter(d -> !d.getFechaInicio().isAfter(hoy)
                && (d.getFechaFin() == null || !d.getFechaFin().isBefore(hoy)))
            .max(Comparator.comparing(DelegacionFuncion::getFechaInicio))
            .map(d -> {
                var empDelegado = d.getAsignacionDelegada().getEmpleado();
                if (empDelegado == null) return null;
                var usuarioDelegado = usuarioRepository
                    .findByEmpleadoIdEmpleadoAndActivoTrue(empDelegado.getIdEmpleado())
                    .orElse(null);
                if (usuarioDelegado == null) return null;
                return new DelegacionResueltaDTO(
                    usuarioDelegado.getIdUsuario(),
                    d.getIdDelegacion(),
                    idUsuario,
                    nombreCompleto(empDelegado),
                    usuario.getNombres() + " " + usuario.getApellidos(),
                    d.getTipo(),
                    d.getFechaInicio(),
                    d.getFechaFin()
                );
            })
            .orElse(null);
    }

    /**
     * Devuelve los IDs de usuario que me delegaron sus funciones activamente.
     * Útil para bandejas: incluir documentos de usuarios que me delegaron.
     */
    @Transactional(readOnly = true)
    public List<Integer> usuariosQueMeDelegaron(Integer idUsuario) {
        if (idUsuario == null) return List.of();
        var usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null || usuario.getEmpleado() == null) return List.of();

        LocalDate hoy = LocalDate.now();
        return delegacionRepository.findAll().stream()
            .filter(d -> ACTIVA.equals(d.getEstado()))
            .filter(d -> !d.getFechaInicio().isAfter(hoy)
                && (d.getFechaFin() == null || !d.getFechaFin().isBefore(hoy)))
            .filter(d -> {
                var empDelegado = d.getAsignacionDelegada().getEmpleado();
                if (empDelegado == null) return false;
                var uDelegado = usuarioRepository
                    .findByEmpleadoIdEmpleadoAndActivoTrue(empDelegado.getIdEmpleado())
                    .orElse(null);
                return uDelegado != null && idUsuario.equals(uDelegado.getIdUsuario());
            })
            .map(d -> {
                var empOrigen = d.getAsignacionOrigen().getEmpleado();
                if (empOrigen == null) return null;
                return usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(empOrigen.getIdEmpleado())
                    .map(Usuario::getIdUsuario)
                    .orElse(null);
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private DelegacionFuncionDTO toDTO(DelegacionFuncion d) {
        var origen = d.getAsignacionOrigen();
        var delegada = d.getAsignacionDelegada();
        return new DelegacionFuncionDTO(
            d.getIdDelegacion(),
            origen.getIdAsignacion(),
            origen.getEmpleado() != null ? origen.getEmpleado().getIdEmpleado() : null,
            origen.getEmpleado() != null ? nombreCompleto(origen.getEmpleado()) : null,
            origen.getPuesto() != null ? origen.getPuesto().getNombre() : null,
            origen.getUnidadOrganizacional() != null ? origen.getUnidadOrganizacional().getNombre() : null,
            delegada.getIdAsignacion(),
            delegada.getEmpleado() != null ? delegada.getEmpleado().getIdEmpleado() : null,
            delegada.getEmpleado() != null ? nombreCompleto(delegada.getEmpleado()) : null,
            delegada.getPuesto() != null ? delegada.getPuesto().getNombre() : null,
            delegada.getUnidadOrganizacional() != null ? delegada.getUnidadOrganizacional().getNombre() : null,
            d.getFechaInicio(),
            d.getFechaFin(),
            d.getTipo(),
            d.getAlcance(),
            d.getDocumentoRespaldoId(),
            d.getEstado(),
            d.getObservacion(),
            d.getCreadoPor()
        );
    }

    private static String nombreCompleto(Empleado e) {
        return (e.getNombres() != null ? e.getNombres() : "")
            + " " + (e.getApellidos() != null ? e.getApellidos() : "");
    }
}