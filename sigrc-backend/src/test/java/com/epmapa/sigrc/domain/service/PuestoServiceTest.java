package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.PuestoRequest;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import com.epmapa.sigrc.domain.repository.VersionManualRepository;
import jakarta.persistence.EntityNotFoundException;
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
 * Pruebas obligatorias §37 — Puestos: crear perfil completo (funciones,
 * formación, experiencia, capacitación, productos, interfaces), consultar
 * requisitos, código duplicado, desactivar.
 */
@ExtendWith(MockitoExtension.class)
class PuestoServiceTest {

    @Mock private PuestoRepository puestoRepository;
    @Mock private UnidadOrganizacionalRepository unidadRepository;
    @Mock private VersionManualRepository versionRepository;
    @Mock private AuditoriaEventos auditoriaEventos;

    private PuestoService service;

    @BeforeEach
    void setUp() {
        service = new PuestoService(puestoRepository, unidadRepository, versionRepository, auditoriaEventos);
    }

    private PuestoRequest perfilCompleto() {
        return new PuestoRequest(
            "TH-JTH-001", "JEFE DE TALENTO HUMANO", 1, "EJECUCIÓN DE PROCESOS", "PROCESO",
            "SP5", "Dirigir el subsistema de talento humano", "Tercer nivel", 48,
            true, true, 1, LocalDate.of(2023, 1, 1), null, 1, null,
            List.of(PuestoFuncion.builder().descripcion("Planificar la gestión de talento humano")
                .tipo("ESENCIAL").orden(1).build()),
            List.of(PuestoFormacion.builder().nivelInstruccion("Tercer nivel")
                .tituloArea("Administración de Empresas").obligatorio(true).build()),
            List.of(PuestoExperiencia.builder().tiempoMeses(48)
                .especificidad("Gestión de talento humano").obligatorio(true).build()),
            List.of(PuestoCapacitacion.builder().nombre("Gestión por competencias")
                .horasRequeridas(40).obligatorio(true).build()),
            List.of(PuestoProducto.builder().descripcion("Plan anual de talento humano").orden(1).build()),
            List.of(PuestoInterfaz.builder().descripcion("Dirección Administrativa").tipoInterfaz("INTERNA").build())
        );
    }

    @Test
    void crearPuestoConPerfilCompleto() {
        var unidad = UnidadOrganizacional.builder().idUnidad(1).nombre("Jefatura de Talento Humano").build();
        when(puestoRepository.existsByCodigo("TH-JTH-001")).thenReturn(false);
        when(unidadRepository.getReferenceById(1)).thenReturn(unidad);
        when(puestoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var creado = service.crear(perfilCompleto());

        assertThat(creado.idPuesto()).isNull();
        assertThat(creado.codigo()).isEqualTo("TH-JTH-001");
        assertThat(creado.idUnidad()).isEqualTo(1);
        assertThat(creado.esJefatura()).isTrue();
        assertThat(creado.esResponsableUnidad()).isTrue();
        assertThat(creado.activo()).isTrue();
        verify(puestoRepository).save(any());
    }

    @Test
    void crearPuestoCodigoDuplicado() {
        when(puestoRepository.existsByCodigo("TH-JTH-001")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(perfilCompleto()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya existe");
    }

    @Test
    void crearPuestoSinCodigoEsRechazado() {
        PuestoRequest sinCodigo = new PuestoRequest(null, "Nombre", null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> service.crear(sinCodigo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("código es obligatorio");
    }

    @Test
    void consultarPerfilDePuesto() {
        var puesto = Puesto.builder().idPuesto(7).codigo("TH-JTH-001").nombre("JEFE DE TALENTO HUMANO").build();
        puesto.getFunciones().add(PuestoFuncion.builder().puesto(puesto).descripcion("Planificar").build());
        when(puestoRepository.findById(7)).thenReturn(Optional.of(puesto));

        var perfil = service.obtenerConPerfil(7);

        assertThat(perfil.getIdPuesto()).isEqualTo(7);
        assertThat(perfil.getFunciones()).hasSize(1);
        assertThat(perfil.getFunciones().get(0).getDescripcion()).isEqualTo("Planificar");
    }

    @Test
    void consultarPuestoInexistenteLanzaExcepcion() {
        when(puestoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerConPerfil(99))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void desactivarPuesto() {
        var puesto = Puesto.builder().idPuesto(7).activo(true).build();
        when(puestoRepository.findById(7)).thenReturn(Optional.of(puesto));
        when(puestoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desactivar(7);

        verify(puestoRepository).save(puesto);
        assertThat(puesto.getActivo()).isFalse();
    }
}