package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.RecepcionDelegadaRequest;
import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas BCC — Recepción delegada y bandeja compartida.
 * Validación de reglas de negocio: RN-01..12, RF-01..15.
 */
@ExtendWith(MockitoExtension.class)
class BandejaCompartidaServiceTest {

    @Mock private CorrespondenciaVinculoRepository vinculoRepository;
    @Mock private CorrespondenciaRepository correspondenciaRepository;
    @Mock private CorrespondenciaLecturaUsuarioRepository lecturaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AsignacionPuestoRepository asignacionRepository;
    @Mock private CorrespondenciaDestinatarioRepository destinatarioRepository;
    @Mock private CorrespondenciaHistorialRepository historialRepository;
    @Mock private CorrespondenciaRecepcionIdempotenteRepository idempotenteRepository;
    @Mock private NotificacionWebSocketService notificacionService;
    @Mock private AuditoriaEventos auditoriaEventos;

    private RecepcionDelegadaService recepcionService;
    private BandejaCompartidaService bandejaService;

    private Usuario asistente;
    private Usuario titular;
    private Puesto puestoAsistente;
    private Puesto puestoTitular;
    private AsignacionPuesto asignacionAsistente;
    private AsignacionPuesto asignacionTitular;
    private CorrespondenciaVinculoAsistente vinculo;
    private Correspondencia documento;

    @BeforeEach
    void setUp() {
        recepcionService = new RecepcionDelegadaService(
            correspondenciaRepository, destinatarioRepository, historialRepository,
            vinculoRepository, idempotenteRepository, usuarioRepository,
            asignacionRepository, notificacionService, auditoriaEventos);

        bandejaService = new BandejaCompartidaService(
            vinculoRepository, correspondenciaRepository, lecturaRepository,
            usuarioRepository, asignacionRepository, auditoriaEventos,
            null); // CorrespondenciaService no necesario para las pruebas de lógica

        // Setup fixtures
        puestoAsistente = Puesto.builder().idPuesto(10).nombre("Secretaria de Jefatura").esJefatura(false).build();
        puestoTitular = Puesto.builder().idPuesto(20).nombre("Jefe de Talento Humano").esJefatura(true).build();

        asistente = Usuario.builder().idUsuario(100).username("maria.garcia")
            .nombres("María").apellidos("García").activo(true)
            .empleado(Empleado.builder().idEmpleado(300).build())
            .build();

        titular = Usuario.builder().idUsuario(200).username("juan.perez")
            .nombres("Juan").apellidos("Pérez").activo(true)
            .empleado(Empleado.builder().idEmpleado(400).build())
            .build();

        asignacionAsistente = AsignacionPuesto.builder().idAsignacion(1)
            .empleado(asistente.getEmpleado())
            .puesto(puestoAsistente)
            .esPrincipal(true).estado("ACTIVA")
            .fechaInicio(LocalDate.now().minusDays(30))
            .build();

        asignacionTitular = AsignacionPuesto.builder().idAsignacion(2)
            .empleado(titular.getEmpleado())
            .puesto(puestoTitular)
            .esPrincipal(true).estado("ACTIVA")
            .fechaInicio(LocalDate.now().minusDays(365))
            .build();

        vinculo = CorrespondenciaVinculoAsistente.builder()
            .idVinculo(1)
            .puestoAsistente(puestoAsistente)
            .puestoTitular(puestoTitular)
            .puedeRecepcionar(true)
            .puedeOrganizar(false)
            .puedeSeguimiento(true)
            .vigenteDesde(LocalDate.now().minusDays(10))
            .vigenteHasta(null)
            .activo(true)
            .autorizadoPor(Usuario.builder().idUsuario(1).username("admin").build())
            .build();

        documento = Correspondencia.builder()
            .idCorrespondencia(500)
            .numeroInterno("CORR-2025-001")
            .asunto("Solicitud de material de oficina")
            .estado("RECIBIDO")
            .sentido("INGRESO")
            .activo(true)
            .build();
    }

    // ─── RN-01: Recepción delegada con vínculo vigente ───

    @Test
    void recepcionar_conVinculoVigente_registraRecepcion() {
        // Given
        when(idempotenteRepository.findByIdempotencyKey("abc-123")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.of(vinculo));
        when(correspondenciaRepository.findById(500)).thenReturn(Optional.of(documento));
        when(destinatarioRepository.findByCorrespondenciaIdCorrespondencia(500))
            .thenReturn(List.of());
        when(destinatarioRepository.save(any())).thenReturn(null);
        when(usuarioRepository.findByPuestoVigente(20)).thenReturn(List.of(titular));
        when(correspondenciaRepository.save(any())).thenReturn(documento);
        when(historialRepository.save(any())).thenReturn(null);
        when(idempotenteRepository.save(any())).thenReturn(null);

        var req = new RecepcionDelegadaRequest(500, 20, "abc-123", "Recibido en ventanilla");

        // When
        var result = recepcionService.recepcionar(req, 100);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdCorrespondencia()).isEqualTo(500);
        verify(destinatarioRepository, atLeastOnce()).save(any());
        verify(historialRepository).save(any());
        verify(idempotenteRepository).save(any());
        verify(notificacionService).notificarAsignacion(eq(200), any(), any(), any(), eq(500));
    }

    // ─── RN-02: Rechazo sin vínculo ───

    @Test
    void recepcionar_sinVinculo_lanzaSecurityException() {
        when(idempotenteRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.empty());

        var req = new RecepcionDelegadaRequest(500, 20, "key-1", null);

        assertThatThrownBy(() -> recepcionService.recepcionar(req, 100))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("No tiene vínculo vigente");
    }

    // ─── RN-03: Rechazo sin permiso de recepción ───

    @Test
    void recepcionar_sinPermisoRecepcion_lanzaSecurityException() {
        var vinculoSinPermiso = CorrespondenciaVinculoAsistente.builder()
            .idVinculo(2).puestoAsistente(puestoAsistente).puestoTitular(puestoTitular)
            .puedeRecepcionar(false).puedeOrganizar(true).puedeSeguimiento(true)
            .vigenteDesde(LocalDate.now().minusDays(5)).activo(true).autorizadoPor(Usuario.builder().idUsuario(1).username("admin").build()).build();

        when(idempotenteRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.of(vinculoSinPermiso));

        var req = new RecepcionDelegadaRequest(500, 20, "key-2", null);

        assertThatThrownBy(() -> recepcionService.recepcionar(req, 100))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("no autoriza recepción");
    }

    // ─── RN-04: Idempotencia ───

    @Test
    void recepcionar_idempotente_noDuplica() {
        var idem = CorrespondenciaRecepcionIdempotente.builder()
            .idempotencyKey("idem-1")
            .correspondenciaId(500)
            .usuarioId(100)
            .puestoTitularId(20)
            .build();

        when(idempotenteRepository.findByIdempotencyKey("idem-1"))
            .thenReturn(Optional.of(idem));
        when(correspondenciaRepository.findById(500)).thenReturn(Optional.of(documento));

        var req = new RecepcionDelegadaRequest(500, 20, "idem-1", null);
        var result = recepcionService.recepcionar(req, 100);

        assertThat(result.getIdCorrespondencia()).isEqualTo(500);
        // No debe intentar crear nuevo destinatario ni historial
        verify(destinatarioRepository, never()).save(any());
        verify(historialRepository, never()).save(any());
    }

    // ─── RN-05: Vínculo vencido ───

    @Test
    void recepcionar_vinculoVencido_lanzaSecurityException() {
        var vinculoVencido = CorrespondenciaVinculoAsistente.builder()
            .idVinculo(3).puestoAsistente(puestoAsistente).puestoTitular(puestoTitular)
            .puedeRecepcionar(true)
            .vigenteDesde(LocalDate.now().minusDays(60))
            .vigenteHasta(LocalDate.now().minusDays(1))
            .activo(true).autorizadoPor(Usuario.builder().idUsuario(1).username("admin").build()).build();

        when(idempotenteRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.empty()); // vencido → findVigenteEntre no lo encuentra

        var req = new RecepcionDelegadaRequest(500, 20, "key-3", null);

        assertThatThrownBy(() -> recepcionService.recepcionar(req, 100))
            .isInstanceOf(SecurityException.class);
    }

    // ─── RN-06: Vínculo inactivo ───

    @Test
    void recepcionar_vinculoInactivo_lanzaSecurityException() {
        var vinculoInactivo = CorrespondenciaVinculoAsistente.builder()
            .idVinculo(4).puestoAsistente(puestoAsistente).puestoTitular(puestoTitular)
            .puedeRecepcionar(true).activo(false).autorizadoPor(Usuario.builder().idUsuario(1).build())
            .vigenteDesde(LocalDate.now().minusDays(5)).build();

        when(idempotenteRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.empty()); // inactivo → findVigenteEntre filtra por activo

        var req = new RecepcionDelegadaRequest(500, 20, "key-4", null);

        assertThatThrownBy(() -> recepcionService.recepcionar(req, 100))
            .isInstanceOf(SecurityException.class);
    }

    // ─── RN-07: Sin asignación de puesto ───

    @Test
    void recepcionar_sinAsignacion_lanzaSecurityException() {
        when(idempotenteRepository.findByIdempotencyKey("key-5")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.empty());

        var req = new RecepcionDelegadaRequest(500, 20, "key-5", null);

        assertThatThrownBy(() -> recepcionService.recepcionar(req, 100))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("no tiene asignación de puesto vigente");
    }

    // ─── RN-08: Documento inactivo ───

    @Test
    void recepcionar_documentoInactivo_lanzaIllegalArgumentException() {
        var docInactivo = Correspondencia.builder()
            .idCorrespondencia(501).numeroInterno("CORR-2025-002")
            .asunto("Anulado").estado("ARCHIVADO").sentido("INGRESO")
            .activo(false).build();

        when(idempotenteRepository.findByIdempotencyKey("key-6")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.of(vinculo));
        when(correspondenciaRepository.findById(501)).thenReturn(Optional.of(docInactivo));

        var req = new RecepcionDelegadaRequest(501, 20, "key-6", null);

        assertThatThrownBy(() -> recepcionService.recepcionar(req, 100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("inactivo");
    }

    // ─── RN-09: Lectura personal no altera estado global ───

    @Test
    void marcarLeidoPersonal_noAlteraEstadoDocumento() {
        when(correspondenciaRepository.findById(500)).thenReturn(Optional.of(documento));
        when(lecturaRepository.existsByCorrespondenciaIdCorrespondenciaAndUsuarioId(500, 100))
            .thenReturn(false);
        when(lecturaRepository.save(any())).thenReturn(null);

        bandejaService.marcarLeidoPersonal(500, 100);

        // El documento no debe cambiar de estado
        assertThat(documento.getEstado()).isEqualTo("RECIBIDO");
        verify(lecturaRepository).save(any());
    }

    // ─── RN-10: Verificación de vinculación ───

    @Test
    void bandejaTitular_sinVinculo_lanzaSecurityException() {
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bandejaService.bandejaTitular(20, 100))
            .isInstanceOf(SecurityException.class);
    }

    // ─── RN-11: Vínculo sin permiso de seguimiento ───

    @Test
    void bandejaTitular_sinPermisoSeguimiento_lanzaSecurityException() {
        var vinculoSinSeguimiento = CorrespondenciaVinculoAsistente.builder()
            .idVinculo(5).puestoAsistente(puestoAsistente).puestoTitular(puestoTitular)
            .puedeRecepcionar(true).puedeOrganizar(false).puedeSeguimiento(false)
            .vigenteDesde(LocalDate.now().minusDays(5)).activo(true).autorizadoPor(Usuario.builder().idUsuario(1).username("admin").build()).build();

        when(usuarioRepository.findById(100)).thenReturn(Optional.of(asistente));
        when(asignacionRepository.findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
            300, "ACTIVA")).thenReturn(Optional.of(asignacionAsistente));
        when(vinculoRepository.findVigenteEntre(10, 20, LocalDate.now()))
            .thenReturn(Optional.of(vinculoSinSeguimiento));

        assertThatThrownBy(() -> bandejaService.bandejaTitular(20, 100))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("no autoriza seguimiento");
    }
}
