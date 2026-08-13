package com.epmapa.sigrc.domain.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas obligatorias §37 — Migración §35 (usuarios → empleados + asignaciones):
 * idempotencia, creación de empleado con identificación sintética, vinculación
 * usuario.empleado_id, asignación vigente desde área+cargo, y modo dryRun sin persistir.
 */
@ExtendWith(MockitoExtension.class)
class MigracionTHServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadOrganizacionalRepository unidadRepository;
    @Mock private PuestoRepository puestoRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private AsignacionPuestoRepository asignacionRepository;
    @Mock private AuditoriaEventos auditoriaEventos;

    private MigracionTHService service;

    @BeforeEach
    void setUp() {
        service = new MigracionTHService(usuarioRepository, unidadRepository, puestoRepository,
            empleadoRepository, asignacionRepository, auditoriaEventos);
    }

    private Area area(String codigo, String nombre) {
        return Area.builder().idArea(1).codigo(codigo).nombre(nombre).activo(true).build();
    }

    private Usuario usuario(Integer id, String username, Area area, String cargo) {
        return Usuario.builder()
            .idUsuario(id)
            .username(username)
            .email(username + "@epmapa.gob.ec")
            .nombres("Juan")
            .apellidos("Pérez")
            .area(area)
            .cargo(cargo)
            .activo(true)
            .build();
    }

    @Test
    void migraUsuarioSinEmpleadoCreandoVinculoYAsignacion() {
        var area = area("TH", "Talento Humano");
        var usuario = usuario(1, "jperez", area, "Técnico de Talento Humano");
        when(usuarioRepository.findAllByActivoTrue()).thenReturn(List.of(usuario));
        when(unidadRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of());
        when(puestoRepository.findAll()).thenReturn(List.of());
        when(empleadoRepository.existsByIdentificacion(any())).thenReturn(false);
        when(empleadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            any(), any())).thenReturn(Optional.empty());
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.migrarUsuarios(false);

        assertThat(resultado.empleadosCreados()).isEqualTo(1);
        assertThat(resultado.asignacionesCreadas()).isEqualTo(1);
        assertThat(resultado.unidadesCreadas()).isEqualTo(1);
        assertThat(resultado.puestosCreados()).isEqualTo(1);
        assertThat(usuario.getEmpleado()).isNotNull();
        assertThat(usuario.getEmpleado().getIdentificacion()).startsWith("MIG-");
        assertThat(usuario.getEmpleado().getCorreoInstitucional()).isEqualTo("jperez@epmapa.gob.ec");
    }

    @Test
    void reutilizaUnidadYPuestoExistentes() {
        var area = area("TH", "Talento Humano");
        var usuario = usuario(1, "jperez", area, "Técnico de Talento Humano");
        var unidad = UnidadOrganizacional.builder().idUnidad(5).codigo("TH").nombre("Talento Humano")
            .tipoUnidad("JEFATURA").activo(true).build();
        var puesto = Puesto.builder().idPuesto(9).codigo("TH-TTH-001").nombre("Técnico de Talento Humano")
            .unidadOrganizacional(unidad).numeroPlazas(2).activo(true).version(1).build();
        when(usuarioRepository.findAllByActivoTrue()).thenReturn(List.of(usuario));
        when(unidadRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of(unidad));
        when(puestoRepository.findAll()).thenReturn(List.of(puesto));
        when(empleadoRepository.existsByIdentificacion(any())).thenReturn(false);
        when(empleadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            any(), any())).thenReturn(Optional.empty());
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.migrarUsuarios(false);

        assertThat(resultado.unidadesCreadas()).isEqualTo(0);
        assertThat(resultado.puestosCreados()).isEqualTo(0);
        assertThat(resultado.asignacionesCreadas()).isEqualTo(1);
        verify(unidadRepository, never()).save(any());
        verify(puestoRepository, never()).save(any());
    }

    @Test
    void saltaUsuariosYaVinculadosYConAsignacionVigente() {
        var area = area("TH", "Talento Humano");
        var empleado = Empleado.builder().idEmpleado(10).identificacion("0000000001").build();
        var usuario = usuario(1, "jperez", area, "Técnico de Talento Humano");
        usuario.setEmpleado(empleado);
        var asignacion = AsignacionPuesto.builder().idAsignacion(20).estado("ACTIVA").esPrincipal(true).build();
        when(usuarioRepository.findAllByActivoTrue()).thenReturn(List.of(usuario));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            any(), any())).thenReturn(Optional.of(asignacion));

        var resultado = service.migrarUsuarios(false);

        assertThat(resultado.empleadosCreados()).isEqualTo(0);
        assertThat(resultado.asignacionesCreadas()).isEqualTo(0);
        assertThat(resultado.yaVinculados()).isEqualTo(1);
        verify(empleadoRepository, never()).save(any());
    }

    @Test
    void dryRunNoPersisteNada() {
        var area = area("TH", "Talento Humano");
        var usuario = usuario(1, "jperez", area, "Técnico de Talento Humano");
        when(usuarioRepository.findAllByActivoTrue()).thenReturn(List.of(usuario));
        when(unidadRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of());
        when(puestoRepository.findAll()).thenReturn(List.of());

        var resultado = service.migrarUsuarios(true);

        assertThat(resultado.empleadosCreados()).isEqualTo(1);
        assertThat(resultado.asignacionesCreadas()).isEqualTo(1);
        verify(empleadoRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
        verify(unidadRepository, never()).save(any());
        verify(puestoRepository, never()).save(any());
        verify(asignacionRepository, never()).save(any());
        assertThat(usuario.getEmpleado()).isNull();
    }

    @Test
    void usuarioSinCargoNoGeneraAsignacion() {
        var area = area("TH", "Talento Humano");
        var usuario = usuario(1, "jperez", area, null);
        when(usuarioRepository.findAllByActivoTrue()).thenReturn(List.of(usuario));
        when(empleadoRepository.existsByIdentificacion(any())).thenReturn(false);
        when(empleadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.migrarUsuarios(false);

        assertThat(resultado.empleadosCreados()).isEqualTo(1);
        assertThat(resultado.asignacionesCreadas()).isEqualTo(0);
        assertThat(resultado.detalles()).hasSize(1);
        assertThat(resultado.detalles().get(0).resultado()).isEqualTo("SIN_CARGO");
    }
}
