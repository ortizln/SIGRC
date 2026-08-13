package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportesTalentoHumanoService {

    private static final String ACTIVA = "ACTIVA";

    private final EmpleadoRepository empleadoRepository;
    private final PuestoRepository puestoRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final SolicitudAusenciaRepository ausenciaRepository;
    private final MovimientoPersonalRepository movimientoRepository;
    private final UnidadOrganizacionalRepository unidadRepository;

    public ReportesTalentoHumanoService(EmpleadoRepository empleadoRepository,
                                        PuestoRepository puestoRepository,
                                        AsignacionPuestoRepository asignacionRepository,
                                        SolicitudAusenciaRepository ausenciaRepository,
                                        MovimientoPersonalRepository movimientoRepository,
                                        UnidadOrganizacionalRepository unidadRepository) {
        this.empleadoRepository = empleadoRepository;
        this.puestoRepository = puestoRepository;
        this.asignacionRepository = asignacionRepository;
        this.ausenciaRepository = ausenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.unidadRepository = unidadRepository;
    }

    // ─────────────────── Distributivo de personal ───────────────────

    @Transactional(readOnly = true)
    public List<DistributivoDTO> distributivo(Integer idUnidad, Integer idPuesto,
                                              String estado, String tipoPersonal) {
        // Asignaciones vigentes (principal + activa)
        var asignaciones = asignacionRepository
            .findByEstadoAndEsPrincipalTrueAndPuestoActivoTrueOrderByFechaInicioDesc(ACTIVA);
        return asignaciones.stream()
            .map(a -> toDistributivo(a))
            .filter(d -> idUnidad == null || Objects.equals(d.idUnidad(), idUnidad))
            .filter(d -> idPuesto == null || Objects.equals(d.idPuesto(), idPuesto))
            .filter(d -> estado == null || estado.isBlank() || Objects.equals(d.estadoLaboral(), estado))
            .filter(d -> tipoPersonal == null || tipoPersonal.isBlank() || Objects.equals(d.tipoPersonal(), tipoPersonal))
            .collect(Collectors.toList());
    }

    private DistributivoDTO toDistributivo(AsignacionPuesto a) {
        var emp = a.getEmpleado();
        var puesto = a.getPuesto();
        var unidad = a.getUnidadOrganizacional();
        return new DistributivoDTO(
            emp != null ? emp.getIdEmpleado() : null,
            emp != null ? emp.getIdentificacion() : null,
            emp != null ? (emp.getNombres() + " " + emp.getApellidos()) : null,
            unidad != null ? unidad.getIdUnidad() : null,
            unidad != null ? unidad.getNombre() : null,
            puesto != null ? puesto.getIdPuesto() : null,
            puesto != null ? puesto.getNombre() : null,
            puesto != null ? puesto.getGrupoOcupacional() : null,
            a.getTipoAsignacion(),
            emp != null ? emp.getFechaIngresoInstitucion() : null,
            emp != null ? emp.getEstadoLaboral() : null,
            emp != null ? emp.getTipoPersonal() : null
        );
    }

    // ─────────────────── Dashboard Talento Humano ───────────────────

    @Transactional(readOnly = true)
    public DashboardTalentoHumanoDTO dashboard() {
        var empleados = empleadoRepository.findAll();
        var asignaciones = asignacionRepository
            .findByEstadoAndEsPrincipalTrueAndPuestoActivoTrueOrderByFechaInicioDesc(ACTIVA);
        var puestos = puestoRepository.findByActivoTrueOrderByNombre();
        var vacantes = puestoRepository.findVacantes();

        LocalDate hoy = LocalDate.now();
        long personalVacaciones = ausenciaRepository
            .findAprobadasVigentes(List.of("VACACION"), hoy).size();
        long personalLicencia = ausenciaRepository
            .findAprobadasVigentes(List.of("LICENCIA", "ENFERMEDAD", "MATERNIDAD", "PATERNIDAD"), hoy).size();
        long capacitaciones = empleadoRepository.countCapacitaciones();
        long movimientosDelMes = movimientoRepository
            .countDesde(YearMonth.now().atDay(1).atStartOfDay());

        long total = empleados.size();
        long activos = empleados.stream()
            .filter(e -> Boolean.TRUE.equals(e.getActivo()) && "ACTIVO".equalsIgnoreCase(e.getEstadoLaboral() == null ? "" : e.getEstadoLaboral()))
            .count();
        long desvinculados = total - activos;

        return new DashboardTalentoHumanoDTO(
            total,
            activos,
            desvinculados,
            asignaciones.size(),
            vacantes.size(),
            personalVacaciones,
            personalLicencia,
            capacitaciones,
            movimientosDelMes,
            agruparPor(asignaciones, a -> {
                var u = a.getUnidadOrganizacional();
                return u != null ? u.getNombre() : "Sin unidad";
            }),
            agruparPor(asignaciones, a -> {
                var p = a.getPuesto();
                return p != null ? p.getNombre() : "Sin puesto";
            }),
            agruparPor(asignaciones, a -> {
                var p = a.getPuesto();
                return p != null && p.getGrupoOcupacional() != null ? p.getGrupoOcupacional() : "Sin grupo";
            }),
            agruparPor(empleados, e -> e.getTipoPersonal() != null && !e.getTipoPersonal().isBlank()
                ? e.getTipoPersonal() : "Sin tipo"),
            agruparPor(empleados, e -> e.getEstadoLaboral() != null && !e.getEstadoLaboral().isBlank()
                ? e.getEstadoLaboral() : "Sin estado")
        );
    }

    private <T> List<DashboardTalentoHumanoDTO.ItemCount> agruparPor(List<T> items,
                                                                     java.util.function.Function<T, String> labelFn) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (var item : items) {
            String label = labelFn.apply(item);
            conteo.merge(label, 1L, Long::sum);
        }
        return conteo.entrySet().stream()
            .map(e -> new DashboardTalentoHumanoDTO.ItemCount(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    // ─────────────────── Matriz persona-puesto ───────────────────

    @Transactional(readOnly = true)
    public MatrizPersonaPuestoDTO matrizPersonaPuesto(Integer idEmpleado) {
        var empleado = empleadoRepository.findById(idEmpleado)
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + idEmpleado));

        var asignacion = asignacionRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(idEmpleado, ACTIVA)
            .orElse(null);
        if (asignacion == null || asignacion.getPuesto() == null)
            throw new IllegalArgumentException("El empleado no tiene una asignación de puesto vigente");

        var puesto = asignacion.getPuesto();
        List<MatrizPersonaPuestoDTO.CriterioMatrizDTO> criterios = new ArrayList<>();
        criterios.add(evaluarInstruccion(puesto, empleado));
        criterios.add(evaluarFormacion(puesto, empleado));
        criterios.add(evaluarExperiencia(puesto, empleado));
        criterios.add(evaluarCapacitacion(puesto, empleado));

        long cumplidos = criterios.stream().filter(c -> "CUMPLE".equals(c.estado())).count();
        long parciales = criterios.stream().filter(c -> "PARCIAL".equals(c.estado())).count();
        long noCumplidos = criterios.stream().filter(c -> "NO_CUMPLE".equals(c.estado())).count();

        var unidad = asignacion.getUnidadOrganizacional();
        return new MatrizPersonaPuestoDTO(
            empleado.getIdEmpleado(),
            empleado.getNombres() + " " + empleado.getApellidos(),
            puesto.getIdPuesto(),
            puesto.getNombre(),
            unidad != null ? unidad.getNombre() : null,
            puesto.getGrupoOcupacional(),
            criterios,
            cumplidos,
            parciales,
            noCumplidos
        );
    }

    private MatrizPersonaPuestoDTO.CriterioMatrizDTO evaluarInstruccion(Puesto puesto, Empleado empleado) {
        String requerido = puesto.getNivelInstruccion();
        if (requerido == null || requerido.isBlank())
            return nuevoCriterio("Nivel de instrucción", "No definido", "No evaluado", "CUMPLE");
        String encontrado = empleado.getFormaciones().stream()
            .map(EmpleadoFormacion::getNivel)
            .filter(Objects::nonNull)
            .max(comparadorNivel())
            .orElse(null);
        if (encontrado == null)
            return nuevoCriterio("Nivel de instrucción", requerido, "Sin formación registrada", "NO_CUMPLE");
        return nuevoCriterio("Nivel de instrucción", requerido, encontrado,
            cumpleNivel(requerido, encontrado) ? "CUMPLE" : "NO_CUMPLE");
    }

    private MatrizPersonaPuestoDTO.CriterioMatrizDTO evaluarFormacion(Puesto puesto, Empleado empleado) {
        var requeridas = puesto.getFormaciones();
        if (requeridas.isEmpty())
            return nuevoCriterio("Formación", "No definida", "No evaluado", "CUMPLE");
        long conArea = requeridas.stream()
            .filter(f -> f.getTituloArea() != null && !f.getTituloArea().isBlank())
            .count();
        if (conArea == 0)
            return nuevoCriterio("Formación", "Requisitos sin área", "No evaluado", "CUMPLE");
        long coincidencias = empleado.getFormaciones().stream()
            .map(EmpleadoFormacion::getTitulo)
            .filter(Objects::nonNull)
            .filter(t -> requeridas.stream().anyMatch(r -> areaCoincide(r.getTituloArea(), t)))
            .count();
        String req = requeridas.stream().map(PuestoFormacion::getTituloArea).collect(Collectors.joining("; "));
        String enc = coincidencias > 0
            ? coincidencias + " título(s) coincidente(s)"
            : "Sin formación del área requerida";
        String estado = coincidencias >= conArea ? "CUMPLE" : coincidencias > 0 ? "PARCIAL" : "NO_CUMPLE";
        return nuevoCriterio("Formación", req, enc, estado);
    }

    private MatrizPersonaPuestoDTO.CriterioMatrizDTO evaluarExperiencia(Puesto puesto, Empleado empleado) {
        Integer mesesRequeridos = puesto.getExperienciaMeses();
        if (mesesRequeridos == null || mesesRequeridos <= 0)
            return nuevoCriterio("Experiencia", "No definida", "No evaluado", "CUMPLE");

        long mesesEmpleado = empleado.getExperiencias().stream()
            .filter(e -> e.getFechaInicio() != null)
            .mapToLong(e -> {
                LocalDate fin = e.getFechaFin() != null ? e.getFechaFin() : LocalDate.now();
                return java.time.temporal.ChronoUnit.MONTHS.between(e.getFechaInicio(), fin);
            })
            .sum();

        String estado = mesesEmpleado >= mesesRequeridos ? "CUMPLE"
            : mesesEmpleado > 0 ? "PARCIAL" : "NO_CUMPLE";
        return nuevoCriterio("Experiencia", mesesRequeridos + " meses", mesesEmpleado + " meses", estado);
    }

    private MatrizPersonaPuestoDTO.CriterioMatrizDTO evaluarCapacitacion(Puesto puesto, Empleado empleado) {
        var requeridas = puesto.getCapacitaciones();
        if (requeridas.isEmpty())
            return nuevoCriterio("Capacitación", "No definida", "No evaluado", "CUMPLE");
        long conNombre = requeridas.stream()
            .filter(c -> c.getNombre() != null && !c.getNombre().isBlank())
            .count();
        long coincidencias = empleado.getCapacitaciones().stream()
            .map(EmpleadoCapacitacion::getNombre)
            .filter(Objects::nonNull)
            .filter(n -> requeridas.stream().anyMatch(r -> textoCoincide(r.getNombre(), n)))
            .count();
        String req = requeridas.stream().map(PuestoCapacitacion::getNombre).collect(Collectors.joining("; "));
        String enc = coincidencias > 0
            ? coincidencias + " capacitación(es) coincidente(s)"
            : "Sin capacitaciones del área requerida";
        String estado = coincidencias >= conNombre ? "CUMPLE" : coincidencias > 0 ? "PARCIAL" : "NO_CUMPLE";
        return nuevoCriterio("Capacitación", req, enc, estado);
    }

    private MatrizPersonaPuestoDTO.CriterioMatrizDTO nuevoCriterio(String criterio, String req, String enc, String estado) {
        return new MatrizPersonaPuestoDTO.CriterioMatrizDTO(criterio, req, enc, estado);
    }

    private boolean cumpleNivel(String requerido, String encontrado) {
        return comparadorNivel().compare(normalizarNivel(encontrado), normalizarNivel(requerido)) >= 0;
    }

    private String normalizarNivel(String nivel) {
        if (nivel == null) return "";
        String n = nivel.toLowerCase();
        if (n.contains("doctor") || n.contains("phd") || n.contains("magister") || n.contains("maestr")) return "5";
        if (n.contains("cuarto")) return "5";
        if (n.contains("licenci") || n.contains("tercer") || n.contains("ingenier") || n.contains("egresado")) return "4";
        if (n.contains("tecnolog")) return "3";
        if (n.contains("bachiller") || n.contains("secundaria")) return "2";
        if (n.contains("basica") || n.contains("primaria")) return "1";
        return "0";
    }

    private Comparator<String> comparadorNivel() {
        return Comparator.comparingInt(s -> Integer.parseInt(normalizarNivel(s)));
    }

    private boolean areaCoincide(String requerida, String encontrado) {
        return textoCoincide(requerida, encontrado);
    }

    private boolean textoCoincide(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.toLowerCase().trim();
        String y = b.toLowerCase().trim();
        return x.contains(y) || y.contains(x)
            || compartirToken(x, y);
    }

    private boolean compartirToken(String a, String b) {
        Set<String> tokensA = new HashSet<>(Arrays.asList(a.split("[^a-záéíóúñ]+")));
        long comunes = Arrays.stream(b.split("[^a-záéíóúñ]+"))
            .filter(t -> t.length() >= 4 && tokensA.contains(t))
            .count();
        return comunes >= 1;
    }
}