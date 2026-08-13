package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.AsignacionRequest;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas obligatorias §37 — Asignaciones:
 * asignar puesto, trasladar funcionario (cerrar anterior + crear nueva),
 * encargar/finalizar, conservar historial y jefatura automática por
 * estructura organizacional.
 */
@ExtendWith(MockitoExtension.class)
class AsignacionPuestoServiceTest {

    private static final String ACTIVA = "ACTIVA";

    @Mock private AsignacionPuestoRepository asignacionRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private PuestoRepository puestoRepository;
    @Mock private UnidadOrganizacionalRepository unidadRepository;
    @Mock private AuditoriaEventos auditoriaEventos;

    private AsignacionPuestoService service;

    @BeforeEach
    void setUp() {
        service = new AsignacionPuestoService(asignacionRepository, empleadoRepository,
            puestoRepository, unidadRepository, auditoriaEventos);
    }

    private Empleado empleado(Integer id) {
        return Empleado.builder().idEmpleado(id).nombres("Empleado").apellidos(String.valueOf(id)).build();
    }

    private UnidadOrganizacional unidad(Integer id) {
        return UnidadOrganizacional.builder().idUnidad(id).nombre("Unidad " + id).activo(true).build();
    }

    private Puesto puesto(Integer id, boolean activo) {
        return Puesto.builder().idPuesto(id).codigo("PU-" + id).nombre("Puesto " + id).activo(activo).build();
    }

    @Test
    void asignarPuestoCreaAsignacionPrincipalActiva() {
        var emp = empleado(1);
        var p = puesto(2, true);
        var u = unidad(5);
        when(empleadoRepository.findById(1)).thenReturn(Optional.of(emp));
        when(puestoRepository.findById(2)).thenReturn(Optional.of(p));
        when(unidadRepository.findById(5)).thenReturn(Optional.of(u));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(1, ACTIVA))
            .thenReturn(Optional.empty());
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var creada = service.asignar(new AsignacionRequest(1, 2, 5, "TITULAR", LocalDate.of(2025, 1, 1), null, null));

        assertThat(creada.estado()).isEqualTo(ACTIVA);
        assertThat(creada.esPrincipal()).isTrue();
        assertThat(creada.idPuesto()).isEqualTo(2);
        assertThat(creada.idUnidad()).isEqualTo(5);
    }

    @Test
    void trasladarFuncionarioCierraAnteriorYConservaHistorial() {
        var emp = empleado(1);
        var pNuevo = puesto(2, true);
        var u = unidad(5);
        var anterior = AsignacionPuesto.builder().idAsignacion(1).empleado(emp)
            .puesto(puesto(1, true)).unidadOrganizacional(u)
            .tipoAsignacion("TITULAR").fechaInicio(LocalDate.of(2020, 1, 1))
            .esPrincipal(true).estado(ACTIVA).build();

        when(empleadoRepository.findById(1)).thenReturn(Optional.of(emp));
        when(puestoRepository.findById(2)).thenReturn(Optional.of(pNuevo));
        when(unidadRepository.findById(5)).thenReturn(Optional.of(u));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(1, ACTIVA))
            .thenReturn(Optional.of(anterior));
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate fechaInicioNueva = LocalDate.of(2025, 6, 1);
        var creada = service.asignar(new AsignacionRequest(1, 2, 5, "TRASLADO", fechaInicioNueva, null, null));

        // La anterior se cierra sin sobrescribirse: FINALIZADA, no principal, fechaFin = día previo.
        assertThat(anterior.getEstado()).isEqualTo("FINALIZADA");
        assertThat(anterior.getEsPrincipal()).isFalse();
        assertThat(anterior.getFechaFin()).isEqualTo(fechaInicioNueva.minusDays(1));
        verify(asignacionRepository).save(anterior);

        // La nueva es la principal vigente.
        assertThat(creada.estado()).isEqualTo(ACTIVA);
        assertThat(creada.esPrincipal()).isTrue();
        assertThat(creada.tipoAsignacion()).isEqualTo("TRASLADO");
    }

    @Test
    void asignarPuestoInactivoEsRechazado() {
        when(empleadoRepository.findById(1)).thenReturn(Optional.of(empleado(1)));
        when(puestoRepository.findById(2)).thenReturn(Optional.of(puesto(2, false)));

        assertThatThrownBy(() -> service.asignar(new AsignacionRequest(1, 2, 5, "TITULAR", null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("puesto está inactivo");
    }

    @Test
    void asignarUnidadInactivaEsRechazada() {
        var u = UnidadOrganizacional.builder().idUnidad(5).activo(false).build();
        when(empleadoRepository.findById(1)).thenReturn(Optional.of(empleado(1)));
        when(puestoRepository.findById(2)).thenReturn(Optional.of(puesto(2, true)));
        when(unidadRepository.findById(5)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.asignar(new AsignacionRequest(1, 2, 5, "TITULAR", null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unidad está inactiva");
    }

    @Test
    void finalizarAsignacion() {
        var asignacion = AsignacionPuesto.builder().idAsignacion(1).empleado(empleado(1))
            .puesto(puesto(1, true)).unidadOrganizacional(unidad(5))
            .tipoAsignacion("ENCARGO").esPrincipal(true).estado(ACTIVA).build();
        when(asignacionRepository.findById(1)).thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var finalizada = service.finalizar(1);

        assertThat(finalizada.estado()).isEqualTo("FINALIZADA");
        assertThat(finalizada.esPrincipal()).isFalse();
        assertThat(finalizada.fechaFin()).isEqualTo(LocalDate.now());
        verify(asignacionRepository).save(asignacion);
    }

    @Test
    void listarPorEmpleadoConservaHistorial() {
        var emp = empleado(1);
        var historica = AsignacionPuesto.builder().idAsignacion(1).empleado(emp)
            .puesto(puesto(1, true)).unidadOrganizacional(unidad(5))
            .tipoAsignacion("TITULAR").esPrincipal(false).estado("FINALIZADA").build();
        var vigente = AsignacionPuesto.builder().idAsignacion(2).empleado(emp)
            .puesto(puesto(2, true)).unidadOrganizacional(unidad(5))
            .tipoAsignacion("TITULAR").esPrincipal(true).estado(ACTIVA).build();
        when(asignacionRepository.findByEmpleadoIdEmpleadoOrderByFechaInicioDesc(1))
            .thenReturn(List.of(vigente, historica));

        var asignaciones = service.listarPorEmpleado(1);

        assertThat(asignaciones).hasSize(2);
        assertThat(asignaciones).extracting(AsignacionPuestoServiceTest::idAsignacion)
            .containsExactly(2, 1);
    }

    private static Integer idAsignacion(com.epmapa.sigrc.domain.dto.AsignacionDTO dto) {
        return dto.idAsignacion();
    }

    @Test
    void jefeInmediatoPorResponsableDeUnidad() {
        var unidad = UnidadOrganizacional.builder().idUnidad(5).nombre("Jefatura TH").responsableAsignacionId(100)
            .activo(true).build();
        var empleadoActual = empleado(1);
        var jefe = empleado(2);
        var asignacionActual = AsignacionPuesto.builder().idAsignacion(10).empleado(empleadoActual)
            .puesto(puesto(1, true)).unidadOrganizacional(unidad).esPrincipal(true).estado(ACTIVA).build();
        var asignacionJefe = AsignacionPuesto.builder().idAsignacion(100).empleado(jefe)
            .puesto(puesto(2, true)).unidadOrganizacional(unidad).esPrincipal(true).estado(ACTIVA).build();

        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(1, ACTIVA))
            .thenReturn(Optional.of(asignacionActual));
        when(asignacionRepository.findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueOrderByFechaInicioDesc(5, ACTIVA))
            .thenReturn(Optional.of(asignacionJefe));

        var info = service.jefeInmediato(1);

        assertThat(info).isNotNull();
        assertThat(info.idJefe()).isEqualTo(2);
        assertThat(info.idUnidad()).isEqualTo(5);
    }

    @Test
    void jefeInmediatoPorPuestoDeJefatura() {
        var unidad = UnidadOrganizacional.builder().idUnidad(5).nombre("Unidad").activo(true).build();
        var empleadoActual = empleado(1);
        var jefe = empleado(2);
        var asignacionActual = AsignacionPuesto.builder().idAsignacion(10).empleado(empleadoActual)
            .puesto(puesto(1, true)).unidadOrganizacional(unidad).esPrincipal(true).estado(ACTIVA).build();
        var pJefatura = Puesto.builder().idPuesto(2).codigo("PU-2").nombre("Jefe").esJefatura(true).activo(true).build();
        var asignacionJefe = AsignacionPuesto.builder().idAsignacion(11).empleado(jefe)
            .puesto(pJefatura).unidadOrganizacional(unidad).esPrincipal(true).estado(ACTIVA).build();

        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(1, ACTIVA))
            .thenReturn(Optional.of(asignacionActual));
        when(asignacionRepository.findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsResponsableUnidadTrueOrderByFechaInicioDesc(5, ACTIVA))
            .thenReturn(Optional.empty());
        when(asignacionRepository.findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsJefaturaTrueOrderByFechaInicioDesc(5, ACTIVA))
            .thenReturn(Optional.of(asignacionJefe));

        var info = service.jefeInmediato(1);

        assertThat(info).isNotNull();
        assertThat(info.idJefe()).isEqualTo(2);
        assertThat(info.puestoNombre()).isEqualTo("Jefe");
    }

    @Test
    void jefeInmediatoSeResuelveDesdeUnidadPadre() {
        var padre = UnidadOrganizacional.builder().idUnidad(9).nombre("Dirección").responsableAsignacionId(200)
            .activo(true).build();
        var unidad = UnidadOrganizacional.builder().idUnidad(5).nombre("Unidad").unidadPadre(padre).activo(true).build();
        var empleadoActual = empleado(1);
        var jefe = empleado(2);
        var asignacionActual = AsignacionPuesto.builder().idAsignacion(10).empleado(empleadoActual)
            .puesto(puesto(1, true)).unidadOrganizacional(unidad).esPrincipal(true).estado(ACTIVA).build();
        var asignacionJefe = AsignacionPuesto.builder().idAsignacion(200).empleado(jefe)
            .puesto(puesto(2, true)).unidadOrganizacional(padre).esPrincipal(true).estado(ACTIVA).build();

        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(1, ACTIVA))
            .thenReturn(Optional.of(asignacionActual));
        when(asignacionRepository.findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsResponsableUnidadTrueOrderByFechaInicioDesc(5, ACTIVA))
            .thenReturn(Optional.empty());
        when(asignacionRepository.findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsJefaturaTrueOrderByFechaInicioDesc(5, ACTIVA))
            .thenReturn(Optional.empty());

        when(unidadRepository.findById(9)).thenReturn(Optional.of(padre));
        when(asignacionRepository.findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueOrderByFechaInicioDesc(9, ACTIVA))
            .thenReturn(Optional.of(asignacionJefe));

        var info = service.jefeInmediato(1);

        assertThat(info).isNotNull();
        assertThat(info.idJefe()).isEqualTo(2);
        assertThat(info.idUnidad()).isEqualTo(9);
    }

    @Test
    void sinAsignacionVigenteNoHayJefe() {
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(1, ACTIVA))
            .thenReturn(Optional.empty());

        assertThat(service.jefeInmediato(1)).isNull();
    }
}