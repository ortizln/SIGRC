package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.UnidadOrganizacionalRequest;
import com.epmapa.sigrc.domain.entity.NivelOrganizacional;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.entity.UnidadOrganizacional;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.NivelOrganizacionalRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas obligatorias §37 — Estructura organizacional:
 * crear unidad raíz, crear subunidad, impedir ciclos, cambiar unidad padre,
 * inactivar unidad, organigrama desde BD.
 */
@ExtendWith(MockitoExtension.class)
class EstructuraOrganizacionalServiceTest {

    @Mock private NivelOrganizacionalRepository nivelRepository;
    @Mock private UnidadOrganizacionalRepository unidadRepository;
    @Mock private AsignacionPuestoRepository asignacionRepository;
    @Mock private PuestoRepository puestoRepository;

    private EstructuraOrganizacionalService service;

    @BeforeEach
    void setUp() {
        service = new EstructuraOrganizacionalService(nivelRepository, unidadRepository, asignacionRepository, puestoRepository);
    }

    private UnidadOrganizacionalRequest request(String codigo, Integer idPadre) {
        return new UnidadOrganizacionalRequest(codigo, "Unidad " + codigo, "U" + codigo,
            "desc", "UNIDAD", null, idPadre, 1);
    }

    @Test
    void crearUnidadRaiz() {
        when(unidadRepository.existsByCodigo("GER")).thenReturn(false);
        when(unidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var creada = service.crearUnidad(request("GER", null));

        assertThat(creada.idUnidad()).isNull();
        assertThat(creada.codigo()).isEqualTo("GER");
        assertThat(creada.activo()).isTrue();
    }

    @Test
    void crearUnidadHija() {
        var padre = UnidadOrganizacional.builder().idUnidad(1).codigo("GER").nombre("GER").activo(true).build();
        when(unidadRepository.existsByCodigo("FIN")).thenReturn(false);
        when(unidadRepository.findById(1)).thenReturn(Optional.of(padre));
        when(unidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var creada = service.crearUnidad(request("FIN", 1));

        assertThat(creada.idUnidadPadre()).isEqualTo(1);
        assertThat(creada.unidadPadreNombre()).isEqualTo("GER");
    }

    @Test
    void crearUnidadCodigoDuplicado() {
        when(unidadRepository.existsByCodigo("GER")).thenReturn(true);

        assertThatThrownBy(() -> service.crearUnidad(request("GER", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya existe");
    }

    @Test
    void crearUnidadConPadreInactivoEsRechazada() {
        var padre = UnidadOrganizacional.builder().idUnidad(1).activo(false).build();
        when(unidadRepository.existsByCodigo("FIN")).thenReturn(false);
        when(unidadRepository.findById(1)).thenReturn(Optional.of(padre));

        assertThatThrownBy(() -> service.crearUnidad(request("FIN", 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("padre está inactiva");
    }

    @Test
    void crearNivelCodigoDuplicado() {
        when(nivelRepository.existsByCodigo("DIR")).thenReturn(true);

        assertThatThrownBy(() -> service.crearNivel("DIR", "Directivo", null, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya existe");
    }

    @Test
    void cambiarUnidadPadreValido() {
        var unidad = UnidadOrganizacional.builder().idUnidad(1).codigo("FIN").activo(true).build();
        var nuevoPadre = UnidadOrganizacional.builder().idUnidad(2).codigo("GER").activo(true).build();
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));
        when(unidadRepository.findById(2)).thenReturn(Optional.of(nuevoPadre));
        when(unidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var actualizada = service.actualizarUnidad(1, request("FIN", 2));

        assertThat(actualizada.idUnidadPadre()).isEqualTo(2);
    }

    @Test
    void impedirCicloJerarquico() {
        var raiz = UnidadOrganizacional.builder().idUnidad(1).codigo("GER").activo(true).build();
        var hijo = UnidadOrganizacional.builder().idUnidad(2).codigo("FIN").unidadPadre(raiz).activo(true).build();
        // Asignar la raíz como padre de su propio descendiente ⇒ ciclo.
        when(unidadRepository.findById(1)).thenReturn(Optional.of(raiz));
        when(unidadRepository.findById(2)).thenReturn(Optional.of(hijo));

        // raiz (1) debe rebotar cuando se intenta colgar de su descendiente (2 → 1)
        UnidadOrganizacionalRequest req = new UnidadOrganizacionalRequest("GER", "Gerencia", "GER", null,
            "GERENCIA", null, 2, 1);

        assertThatThrownBy(() -> service.actualizarUnidad(1, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ciclo jerárquico");
    }

    @Test
    void desactivarUnidadSinHijas() {
        var unidad = UnidadOrganizacional.builder().idUnidad(1).activo(true).build();
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));
        when(unidadRepository.countByUnidadPadreIdUnidad(1)).thenReturn(0L);
        when(unidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desactivarUnidad(1);

        verify(unidadRepository).save(unidad);
        assertThat(unidad.getActivo()).isFalse();
    }

    @Test
    void desactivarUnidadConHijasEsRechazada() {
        var unidad = UnidadOrganizacional.builder().idUnidad(1).activo(true).build();
        when(unidadRepository.findById(1)).thenReturn(Optional.of(unidad));
        when(unidadRepository.countByUnidadPadreIdUnidad(1)).thenReturn(1L);

        assertThatThrownBy(() -> service.desactivarUnidad(1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unidades hijas");
    }

    @Test
    void organigramaSeGeneraDesdeUnidades() {
        var gerencia = UnidadOrganizacional.builder().idUnidad(1).codigo("GER").nombre("Gerencia").build();
        var direccion = UnidadOrganizacional.builder().idUnidad(2).codigo("DIR").nombre("Dirección")
            .unidadPadre(gerencia).build();
        var jefatura = UnidadOrganizacional.builder().idUnidad(3).codigo("JTH").nombre("Jefatura TH")
            .unidadPadre(direccion).build();
        when(unidadRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of(gerencia, direccion, jefatura));
        when(puestoRepository.findByActivoTrueOrderByNombre()).thenReturn(List.of());
        when(asignacionRepository.findByEstadoAndEsPrincipalTrueAndPuestoActivoTrueOrderByFechaInicioDesc("ACTIVA"))
            .thenReturn(List.of());

        var arbol = service.organigrama();

        assertThat(arbol).hasSize(1);
        assertThat(arbol.get(0).idUnidad()).isEqualTo(1);
        assertThat(arbol.get(0).hijos()).hasSize(1);
        assertThat(arbol.get(0).hijos().get(0).hijos()).hasSize(1);
        assertThat(arbol.get(0).hijos().get(0).hijos().get(0).nombre()).isEqualTo("Jefatura TH");
        assertThat(arbol.get(0).plazas()).isEqualTo(0);
        assertThat(arbol.get(0).vacantes()).isEqualTo(0);
    }

    @Test
    void organigramaConsolidaPlazasYResponsable() {
        var gerencia = UnidadOrganizacional.builder().idUnidad(1).codigo("GER").nombre("Gerencia")
            .responsableAsignacionId(10).build();
        var puesto = Puesto.builder().idPuesto(1).codigo("P1").nombre("Director General")
            .esResponsableUnidad(true).numeroPlazas(3)
            .unidadOrganizacional(gerencia).build();
        var empleado = com.epmapa.sigrc.domain.entity.Empleado.builder()
            .idEmpleado(1).nombres("Ana").apellidos("Paz").build();
        var asignacion = com.epmapa.sigrc.domain.entity.AsignacionPuesto.builder()
            .idAsignacion(10).estado("ACTIVA").esPrincipal(true)
            .empleado(empleado).puesto(puesto).unidadOrganizacional(gerencia).build();
        when(unidadRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of(gerencia));
        when(puestoRepository.findByActivoTrueOrderByNombre()).thenReturn(List.of(puesto));
        when(asignacionRepository.findByEstadoAndEsPrincipalTrueAndPuestoActivoTrueOrderByFechaInicioDesc("ACTIVA"))
            .thenReturn(List.of(asignacion));

        var arbol = service.organigrama();

        assertThat(arbol).hasSize(1);
        assertThat(arbol.get(0).plazas()).isEqualTo(3);
        assertThat(arbol.get(0).plazasOcupadas()).isEqualTo(1);
        assertThat(arbol.get(0).vacantes()).isEqualTo(2);
        assertThat(arbol.get(0).responsable()).isEqualTo("Ana Paz");
        assertThat(arbol.get(0).puestoResponsable()).isEqualTo("Director General");
    }

    @Test
    void obtenerUnidadInexistenteLanzaExcepcion() {
        when(unidadRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerUnidad(99))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void desactivarNivel() {
        var nivel = NivelOrganizacional.builder().idNivel(1).activo(true).build();
        when(nivelRepository.findById(1)).thenReturn(Optional.of(nivel));
        when(nivelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desactivarNivel(1);

        verify(nivelRepository).save(nivel);
        assertThat(nivel.getActivo()).isFalse();
    }
}