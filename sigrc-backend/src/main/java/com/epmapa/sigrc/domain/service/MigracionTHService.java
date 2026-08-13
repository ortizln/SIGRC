package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.MigracionTHResultadoDTO;
import com.epmapa.sigrc.domain.dto.MigracionTHResultadoDTO.DetalleMigracionDTO;
import com.epmapa.sigrc.domain.entity.AsignacionPuesto;
import com.epmapa.sigrc.domain.entity.Area;
import com.epmapa.sigrc.domain.entity.Empleado;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.entity.UnidadOrganizacional;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Migración §35 — usuarios → empleados + asignaciones (Fases 1 a 5).
 *
 * Idempotente: cada ejecución solo crea lo que falta (empleados sin vincular,
 * asignaciones sin crear). Nunca borra ni toca autenticación ni los campos
 * antiguos usuario.area / usuario.cargo.
 *
 * Modo dryRun: calcula y reporta sin persistir nada (seguro para producción).
 */
@Service
public class MigracionTHService {

    private static final String ACTIVA = "ACTIVA";
    private static final String TITULAR = "TITULAR";
    private static final int CODIGO_MAX = 20;

    private final UsuarioRepository usuarioRepository;
    private final UnidadOrganizacionalRepository unidadRepository;
    private final PuestoRepository puestoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final AuditoriaEventos auditoriaEventos;

    public MigracionTHService(UsuarioRepository usuarioRepository,
                              UnidadOrganizacionalRepository unidadRepository,
                              PuestoRepository puestoRepository,
                              EmpleadoRepository empleadoRepository,
                              AsignacionPuestoRepository asignacionRepository,
                              AuditoriaEventos auditoriaEventos) {
        this.usuarioRepository = usuarioRepository;
        this.unidadRepository = unidadRepository;
        this.puestoRepository = puestoRepository;
        this.empleadoRepository = empleadoRepository;
        this.asignacionRepository = asignacionRepository;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional
    public MigracionTHResultadoDTO migrarUsuarios(boolean dryRun) {
        var usuarios = usuarioRepository.findAllByActivoTrue();
        var unidadesExistentes = new ArrayList<>(unidadRepository.findAllByOrderByOrdenAsc());
        var puestosExistentes = new ArrayList<>(puestoRepository.findAll());

        var unidadesPorNombre = new HashMap<String, UnidadOrganizacional>();
        for (var u : unidadesExistentes) unidadesPorNombre.putIfAbsent(normalizar(u.getNombre()), u);

        var puestosPorClave = new HashMap<String, Puesto>();
        for (var p : puestosExistentes) {
            if (p.getUnidadOrganizacional() == null) continue;
            String clave = unidadClave(p.getUnidadOrganizacional()) + "|" + normalizar(p.getNombre());
            puestosPorClave.putIfAbsent(clave, p);
        }

        Map<String, Long> plazasPorClave = usuarios.stream()
            .filter(u -> u.getArea() != null && u.getCargo() != null && !u.getCargo().isBlank())
            .collect(Collectors.groupingBy(
                u -> claveAreaCargo(u.getArea().getNombre(), u.getCargo()),
                Collectors.counting()));

        int empleadosCreados = 0, asignacionesCreadas = 0, unidadesCreadas = 0, puestosCreados = 0;
        int yaVinculados = 0, conErrores = 0;
        List<DetalleMigracionDTO> detalles = new ArrayList<>();

        for (var usuario : usuarios) {
            try {
                var empleado = usuario.getEmpleado();
                boolean empleadoNuevo = false;
                if (empleado == null) {
                    empleadosCreados++;
                    if (dryRun) {
                        empleado = Empleado.builder().build();
                        empleadoNuevo = true;
                    } else {
                        empleado = crearEmpleado(usuario);
                        empleado = empleadoRepository.save(empleado);
                        usuario.setEmpleado(empleado);
                        usuarioRepository.save(usuario);
                        empleadoNuevo = true;
                        auditoriaEventos.registrar("MIGRAR_CREAR_EMPLEADO", "REGISTRO", "empleado",
                            empleado.getIdEmpleado(), null,
                            Map.of("usuario", usuario.getUsername(), "identificacion", empleado.getIdentificacion()), "OK");
                    }
                } else {
                    yaVinculados++;
                }

                var asignacion = asignacionRepository
                    .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                        empleado.getIdEmpleado(), ACTIVA)
                    .orElse(null);

                if (asignacion == null && usuario.getArea() != null
                    && usuario.getCargo() != null && !usuario.getCargo().isBlank()) {
                    var unidad = resolverUnidad(usuario.getArea(), unidadesPorNombre, unidadesExistentes);
                    var puesto = resolverPuesto(usuario.getArea(), usuario.getCargo(),
                        unidad, puestosPorClave, puestosExistentes, plazasPorClave);

                    if (!dryRun) {
                        if (unidad.getIdUnidad() == null) {
                            unidadRepository.save(unidad);
                            unidadesExistentes.add(unidad);
                            unidadesPorNombre.putIfAbsent(normalizar(unidad.getNombre()), unidad);
                            unidadesCreadas++;
                        }
                        if (puesto.getIdPuesto() == null) {
                            puestoRepository.save(puesto);
                            puestosExistentes.add(puesto);
                            String clave = unidadClave(unidad) + "|" + normalizar(puesto.getNombre());
                            puestosPorClave.putIfAbsent(clave, puesto);
                            puestosCreados++;
                        }
                        var nueva = AsignacionPuesto.builder()
                            .empleado(empleado)
                            .puesto(puesto)
                            .unidadOrganizacional(unidad)
                            .tipoAsignacion(TITULAR)
                            .esPrincipal(true)
                            .estado(ACTIVA)
                            .fechaInicio(null)
                            .observacion("MIGRADO desde usuario.area+cargo")
                            .build();
                        nueva = asignacionRepository.save(nueva);
                        asignacionesCreadas++;
                        auditoriaEventos.registrar("MIGRAR_CREAR_ASIGNACION", "REGISTRO", "asignacion_puesto",
                            nueva.getIdAsignacion(), null,
                            Map.of("usuario", usuario.getUsername(), "idEmpleado", empleado.getIdEmpleado(),
                                "idPuesto", puesto.getIdPuesto(), "idUnidad", unidad.getIdUnidad()), "OK");
                        detalles.add(new DetalleMigracionDTO(usuario.getIdUsuario(), usuario.getUsername(),
                            "OK", "Empleado + asignación creados", empleado.getIdEmpleado(), nueva.getIdAsignacion()));
                    } else {
                        if (unidad.getIdUnidad() == null) {
                            unidadesPorNombre.putIfAbsent(normalizar(unidad.getNombre()), unidad);
                            unidadesExistentes.add(unidad);
                            unidadesCreadas++;
                        }
                        if (puesto.getIdPuesto() == null) {
                            String clave = unidadClave(unidad) + "|" + normalizar(puesto.getNombre());
                            puestosPorClave.putIfAbsent(clave, puesto);
                            puestosExistentes.add(puesto);
                            puestosCreados++;
                        }
                        asignacionesCreadas++;
                        detalles.add(new DetalleMigracionDTO(usuario.getIdUsuario(), usuario.getUsername(),
                            "OK", "Crearía empleado + asignación", null, null));
                    }
                } else if (asignacion == null) {
                    detalles.add(new DetalleMigracionDTO(usuario.getIdUsuario(), usuario.getUsername(),
                        "SIN_CARGO", "Sin área/cargo para asignar", null, null));
                } else {
                    detalles.add(new DetalleMigracionDTO(usuario.getIdUsuario(), usuario.getUsername(),
                        "YA_ASIGNADO", "Ya tiene asignación vigente", empleado.getIdEmpleado(), asignacion.getIdAsignacion()));
                }
            } catch (Exception e) {
                conErrores++;
                detalles.add(new DetalleMigracionDTO(usuario.getIdUsuario(), usuario.getUsername(),
                    "ERROR", e.getMessage(), usuario.getEmpleado() != null ? usuario.getEmpleado().getIdEmpleado() : null, null));
            }
        }

        auditoriaEventos.registrar("MIGRAR_USUARIOS", dryRun ? "CONSULTA" : "REGISTRO", "usuarios", null,
            null, Map.of("dryRun", dryRun, "empleadosCreados", empleadosCreados,
                "asignacionesCreadas", asignacionesCreadas, "unidadesCreadas", unidadesCreadas,
                "puestosCreados", puestosCreados), "OK");

        return new MigracionTHResultadoDTO(dryRun, usuarios.size(), empleadosCreados, asignacionesCreadas,
            unidadesCreadas, puestosCreados, yaVinculados, conErrores, detalles);
    }

    private Empleado crearEmpleado(Usuario usuario) {
        String identificacion = "MIG-" + String.format("%08d", usuario.getIdUsuario());
        String base = identificacion;
        int sufijo = 2;
        while (empleadoRepository.existsByIdentificacion(identificacion)) {
            identificacion = base + "-" + sufijo++;
        }
        return Empleado.builder()
            .tipoIdentificacion("OTRO")
            .identificacion(identificacion)
            .nombres(usuario.getNombres())
            .apellidos(usuario.getApellidos())
            .correoInstitucional(usuario.getEmail())
            .telefono(usuario.getTelefono())
            .estadoLaboral("ACTIVO")
            .activo(true)
            .observaciones("Migrado desde usuario " + usuario.getUsername() + " el " + LocalDate.now()
                + ". Completar datos personales.")
            .build();
    }

    private UnidadOrganizacional resolverUnidad(Area area, Map<String, UnidadOrganizacional> unidadesPorNombre,
                                                List<UnidadOrganizacional> unidadesExistentes) {
        UnidadOrganizacional unidad = unidadesPorNombre.get(normalizar(area.getNombre()));
        if (unidad != null) return unidad;

        String base = normalizarCodigo(area.getCodigo() != null ? area.getCodigo() : area.getNombre());
        String codigo = unicoCodigo(base, unidadesExistentes.stream().map(UnidadOrganizacional::getCodigo).toList());
        var nueva = UnidadOrganizacional.builder()
            .codigo(codigo)
            .nombre(area.getNombre())
            .descripcion(area.getDescripcion())
            .tipoUnidad("UNIDAD")
            .activo(true)
            .build();
        unidadesPorNombre.put(normalizar(area.getNombre()), nueva);
        unidadesExistentes.add(nueva);
        return nueva;
    }

    private Puesto resolverPuesto(Area area, String cargo, UnidadOrganizacional unidad,
                                  Map<String, Puesto> puestosPorClave, List<Puesto> puestosExistentes,
                                  Map<String, Long> plazasPorClave) {
        String clave = unidadClave(unidad) + "|" + normalizar(cargo);
        Puesto existente = puestosPorClave.get(clave);
        if (existente != null) return existente;

        Long plazas = plazasPorClave.getOrDefault(claveAreaCargo(area.getNombre(), cargo), 1L);
        String base = normalizarCodigo((unidad.getCodigo() != null ? unidad.getCodigo() : "MIG") + "-" + cargo);
        String codigo = unicoCodigo(base, puestosExistentes.stream().map(Puesto::getCodigo).toList());
        var nuevo = Puesto.builder()
            .codigo(codigo)
            .nombre(cargo)
            .unidadOrganizacional(unidad)
            .numeroPlazas(plazas.intValue())
            .activo(true)
            .version(1)
            .build();
        puestosPorClave.put(clave, nuevo);
        puestosExistentes.add(nuevo);
        return nuevo;
    }

    private String unicoCodigo(String base, List<String> codigosUsados) {
        String baseCorta = base.substring(0, Math.min(CODIGO_MAX - 3, base.length()));
        String candidato = baseCorta;
        int sufijo = 1;
        while (codigosUsados.contains(candidato.toUpperCase()) || codigosUsados.contains(candidato)) {
            String suf = "-" + sufijo++;
            candidato = (baseCorta + suf).substring(0, Math.min(CODIGO_MAX, baseCorta.length() + suf.length()));
        }
        return candidato;
    }

    private static String claveAreaCargo(String area, String cargo) {
        return normalizar(area) + "|" + normalizar(cargo);
    }

    private static String unidadClave(UnidadOrganizacional u) {
        return u.getIdUnidad() != null ? String.valueOf(u.getIdUnidad()) : normalizar(u.getNombre());
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    private static String normalizarCodigo(String s) {
        String n = normalizar(s);
        return n.isEmpty() ? "MIG" : n.substring(0, Math.min(CODIGO_MAX, n.length())).toUpperCase();
    }
}
