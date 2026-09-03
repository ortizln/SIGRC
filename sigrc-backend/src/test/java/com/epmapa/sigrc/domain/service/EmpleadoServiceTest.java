package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.EmpleadoRequest;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.AreaRepository;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.RolRepository;
import com.epmapa.sigrc.domain.repository.UsuarioPermisoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas obligatorias §37 — Empleados y Seguridad de información (§30/§31):
 * crear empleado sin usuario, impedir identificación duplicada, desvincular
 * sin borrar historial, y control de acceso al expediente (confidenciales
 * ocultos para auto-consulta, 403 para ajenos, acceso completo para
 * ADMIN/TALENTO_HUMANO).
 */
@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioPermisoRepository usuarioPermisoRepository;
    @Mock private RolRepository rolRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaService auditoriaService;
    @Mock private AuditoriaEventos auditoriaEventos;
    @Mock private HttpServletRequest request;

    private EmpleadoService service;

    @BeforeEach
    void setUp() {
        service = new EmpleadoService(empleadoRepository, usuarioRepository, usuarioPermisoRepository,
            rolRepository, areaRepository, passwordEncoder, auditoriaService, auditoriaEventos);
    }

    private Empleado empleadoConDocumentos() {
        var empleado = Empleado.builder().idEmpleado(10).identificacion("1712345678")
            .nombres("Juan").apellidos("Pérez").activo(true).build();
        empleado.getDocumentos().add(EmpleadoDocumento.builder()
            .tipo("NOMBRAMIENTO").confidencial(false).nivelAcceso("PUBLICO_INSTITUCIONAL").build());
        empleado.getDocumentos().add(EmpleadoDocumento.builder()
            .tipo("INFORME_MEDICO").confidencial(true).nivelAcceso("RESTRINGIDO").build());
        return empleado;
    }

    private Usuario usuarioBasico() {
        return Usuario.builder().idUsuario(1).username("jperez")
            .rol(Rol.builder().codigo("FUNCIONARIO").build()).build();
    }

    private EmpleadoRequest requestCompleto() {
        return new EmpleadoRequest("C", "1712345678", "Juan", "Pérez", null, null, null, null,
            null, null, null, null, null, "SERVIDOR_PUBLICO", "ACTIVO", null, null, null,
            List.of(EmpleadoFormacion.builder().nivel("Tercer nivel").titulo("Ingeniería en Sistemas").build()),
            List.of(), List.of(), List.of(),
            null, null, null, null, null, null);
    }

    // ---------- Creación / duplicados / desvinculación ----------

    @Test
    void crearEmpleadoSinUsuario() {
        when(empleadoRepository.existsByIdentificacion("1712345678")).thenReturn(false);
        when(empleadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var empleado = service.crear(requestCompleto());

        assertThat(empleado.identificacion()).isEqualTo("1712345678");
        assertThat(empleado.nombres()).isEqualTo("Juan");
        assertThat(empleado.apellidos()).isEqualTo("Pérez");
        assertThat(empleado.activo()).isTrue();
        verify(empleadoRepository).save(any());
    }

    @Test
    void impedirIdentificacionDuplicadaAlCrear() {
        when(empleadoRepository.existsByIdentificacion("1712345678")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(requestCompleto()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya está registrada");
    }

    @Test
    void impedirIdentificacionDuplicadaAlActualizar() {
        var existente = Empleado.builder().idEmpleado(10).identificacion("0000000001").activo(true).build();
        when(empleadoRepository.findById(10)).thenReturn(Optional.of(existente));
        when(empleadoRepository.existsByIdentificacion("1712345678")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(10, requestCompleto()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya está registrada");
    }

    @Test
    void desvincularSinBorrarHistorial() {
        var empleado = Empleado.builder().idEmpleado(10).identificacion("1712345678").activo(true).build();
        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desactivar(10);

        verify(empleadoRepository).save(empleado);
        assertThat(empleado.getActivo()).isFalse();
    }

    // ---------- Seguridad del expediente (§30) ----------

    @Test
    void funcionarioAjenoNoAccedeAlExpediente() {
        var empleado = empleadoConDocumentos();
        var otro = Empleado.builder().idEmpleado(99).identificacion("9999").build();
        var usuario = usuarioBasico();
        usuario.setEmpleado(otro);

        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioPermisoRepository.findByUsuarioIdUsuarioAndModuloAndActivoTrue(1, "TALENTO_HUMANO"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerConExpedienteAutorizado(10, 1, "jperez", request))
            .isInstanceOf(AccessDeniedException.class);
        verify(auditoriaService).registrar(any(), any(), any(), any(), any(), any(), any(), any(),
            any(), org.mockito.ArgumentMatchers.eq("DENEGADO"));
    }

    @Test
    void propioEmpleadoVeExpedienteConConfidencialesOcultos() {
        var empleado = empleadoConDocumentos();
        var usuario = usuarioBasico();
        usuario.setEmpleado(empleado);

        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioPermisoRepository.findByUsuarioIdUsuarioAndModuloAndActivoTrue(1, "TALENTO_HUMANO"))
            .thenReturn(Optional.empty());

        var resultado = service.obtenerConExpedienteAutorizado(10, 1, "jperez", request);

        assertThat(resultado.getDocumentos()).hasSize(1);
        assertThat(resultado.getDocumentos().get(0).getTipo()).isEqualTo("NOMBRAMIENTO");
    }

    @Test
    void adminVeExpedienteCompletoYAuditaConfidencial() {
        var empleado = empleadoConDocumentos();
        var usuario = usuarioBasico();
        usuario.setEmpleado(null);
        usuario.setRol(Rol.builder().codigo("ADMIN").build());

        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        var resultado = service.obtenerConExpedienteAutorizado(10, 1, "admin", request);

        assertThat(resultado.getDocumentos()).hasSize(2);
        verify(auditoriaEventos).registrar(org.mockito.ArgumentMatchers.eq("VER_DOCUMENTO_CONFIDENCIAL"),
            any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq("OK"), any());
    }

    @Test
    void usuarioConPermisoTHVeExpedienteCompleto() {
        var empleado = empleadoConDocumentos();
        var otro = Empleado.builder().idEmpleado(99).identificacion("9999").build();
        var usuario = usuarioBasico();
        usuario.setEmpleado(otro);

        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioPermisoRepository.findByUsuarioIdUsuarioAndModuloAndActivoTrue(1, "TALENTO_HUMANO"))
            .thenReturn(Optional.of(UsuarioPermiso.builder().modulo("TALENTO_HUMANO").build()));

        var resultado = service.obtenerConExpedienteAutorizado(10, 1, "thuser", request);

        assertThat(resultado.getDocumentos()).hasSize(2);
    }

    @Test
    void autoConsultaRequiereEmpleadoVinculado() {
        var usuario = usuarioBasico();
        usuario.setEmpleado(null);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.obtenerMiExpediente(1, "jperez", request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no está vinculado");
    }

    @Test
    void autoConsultaDelPropioExpedienteOcultaConfidenciales() {
        var empleado = empleadoConDocumentos();
        var usuario = usuarioBasico();
        usuario.setEmpleado(empleado);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findById(10)).thenReturn(Optional.of(empleado));
        when(usuarioPermisoRepository.findByUsuarioIdUsuarioAndModuloAndActivoTrue(1, "TALENTO_HUMANO"))
            .thenReturn(Optional.empty());

        var resultado = service.obtenerMiExpediente(1, "jperez", request);

        assertThat(resultado.getDocumentos()).hasSize(1);
    }
}