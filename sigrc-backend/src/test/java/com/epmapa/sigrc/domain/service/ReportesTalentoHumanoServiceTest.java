package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.MatrizPersonaPuestoDTO;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.MovimientoPersonalRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.SolicitudAusenciaRepository;
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
import static org.mockito.Mockito.when;

/**
 * Pruebas obligatorias §37 — Matriz persona-puesto (§26):
 * comparación de requisitos del puesto contra el expediente, con estados
 * CUMPLE / PARCIAL / NO_CUMPLE, sin bloquear procesos.
 */
@ExtendWith(MockitoExtension.class)
class ReportesTalentoHumanoServiceTest {

    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private PuestoRepository puestoRepository;
    @Mock private AsignacionPuestoRepository asignacionRepository;
    @Mock private SolicitudAusenciaRepository ausenciaRepository;
    @Mock private MovimientoPersonalRepository movimientoRepository;
    @Mock private UnidadOrganizacionalRepository unidadRepository;

    private ReportesTalentoHumanoService service;

    @BeforeEach
    void setUp() {
        service = new ReportesTalentoHumanoService(empleadoRepository, puestoRepository,
            asignacionRepository, ausenciaRepository, movimientoRepository, unidadRepository);
    }

    private Empleado empleado() {
        return Empleado.builder().idEmpleado(1).identificacion("1712345678")
            .nombres("Juan").apellidos("Pérez").build();
    }

    private void stubAsignacionVigente(Empleado empleado, Puesto puesto) {
        var asignacion = AsignacionPuesto.builder().idAsignacion(1).empleado(empleado).puesto(puesto)
            .unidadOrganizacional(UnidadOrganizacional.builder().idUnidad(5).nombre("Jefatura TH").build())
            .esPrincipal(true).estado("ACTIVA").build();
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            empleado.getIdEmpleado(), "ACTIVA")).thenReturn(Optional.of(asignacion));
    }

    private MatrizPersonaPuestoDTO calcular(int idEmpleado) {
        return service.matrizPersonaPuesto(idEmpleado);
    }

    @Test
    void matrizCumpleTodosLosRequisitos() {
        var empleado = empleado();
        empleado.getFormaciones().add(EmpleadoFormacion.builder()
            .nivel("Ingeniería en Sistemas").titulo("Ingeniería en Sistemas").build());
        empleado.getExperiencias().add(EmpleadoExperiencia.builder()
            .institucion("Empresa X").cargo("Analista").fechaInicio(LocalDate.of(2020, 1, 1)).build());
        empleado.getCapacitaciones().add(EmpleadoCapacitacion.builder()
            .nombre("Seguridad Ocupacional").horas(40).build());

        var puesto = Puesto.builder().idPuesto(2).nombre("JEFE DE TALENTO HUMANO")
            .nivelInstruccion("Tercer nivel").experienciaMeses(24).activo(true).build();
        puesto.getFormaciones().add(PuestoFormacion.builder().tituloArea("Sistemas").obligatorio(true).build());
        puesto.getCapacitaciones().add(PuestoCapacitacion.builder().nombre("Seguridad Ocupacional").build());

        when(empleadoRepository.findById(1)).thenReturn(Optional.of(empleado));
        stubAsignacionVigente(empleado, puesto);

        var matriz = calcular(1);

        assertThat(matriz.cumplidos()).isEqualTo(4);
        assertThat(matriz.parciales()).isZero();
        assertThat(matriz.noCumplidos()).isZero();
        assertThat(matriz.criterios()).allMatch(c -> "CUMPLE".equals(c.estado()));
        assertThat(matriz.puesto()).isEqualTo("JEFE DE TALENTO HUMANO");
        assertThat(matriz.unidad()).isEqualTo("Jefatura TH");
        assertThat(matriz.grupoOcupacional()).isNull();
    }

    @Test
    void matrizNoCumpleCuandoNoHayExpediente() {
        var empleado = empleado();
        var puesto = Puesto.builder().idPuesto(2).nombre("JEFE DE TALENTO HUMANO")
            .nivelInstruccion("Cuarto nivel").experienciaMeses(60).activo(true).build();
        puesto.getFormaciones().add(PuestoFormacion.builder().tituloArea("Derecho").obligatorio(true).build());
        puesto.getCapacitaciones().add(PuestoCapacitacion.builder().nombre("Gestión pública").build());

        when(empleadoRepository.findById(1)).thenReturn(Optional.of(empleado));
        stubAsignacionVigente(empleado, puesto);

        var matriz = calcular(1);

        assertThat(matriz.noCumplidos()).isEqualTo(4);
        assertThat(matriz.cumplidos()).isZero();
    }

    @Test
    void matrizParcialCuandoCoincideSoloParteDeLaFormacion() {
        var empleado = empleado();
        empleado.getFormaciones().add(EmpleadoFormacion.builder()
            .nivel("Tercer nivel").titulo("Ingeniería en Sistemas").build());

        // Sin requisitos de nivel/experiencia/capacitación ⇒ esos 3 cuentan como CUMPLE.
        var puesto = Puesto.builder().idPuesto(2).nombre("TÉCNICO").activo(true).build();
        puesto.getFormaciones().add(PuestoFormacion.builder().tituloArea("Sistemas").obligatorio(true).build());
        puesto.getFormaciones().add(PuestoFormacion.builder().tituloArea("Finanzas").obligatorio(true).build());

        when(empleadoRepository.findById(1)).thenReturn(Optional.of(empleado));
        stubAsignacionVigente(empleado, puesto);

        var matriz = calcular(1);

        assertThat(matriz.parciales()).isEqualTo(1);
        assertThat(matriz.cumplidos()).isEqualTo(3);
    }

    @Test
    void matrizRequiereAsignacionVigente() {
        when(empleadoRepository.findById(1)).thenReturn(Optional.of(empleado()));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            1, "ACTIVA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calcular(1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no tiene una asignación de puesto vigente");
    }
}