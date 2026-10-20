package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * RN-07 BCC — La secretaría vinculada NO puede sumillar, derivar ni
 * cambiar el estado de los documentos de su titular.
 */
@ExtendWith(MockitoExtension.class)
class CorrespondenciaSecretariaGuardTest {

    @Mock private CorrespondenciaRepository repository;
    @Mock private CorrespondenciaDocumentoTipoRepository tipoDocRepository;
    @Mock private CorrespondenciaAdjuntoRepository adjuntoRepository;
    @Mock private CorrespondenciaHistorialRepository historialRepository;
    @Mock private CorrespondenciaRespuestaRepository respuestaRepository;
    @Mock private CorrespondenciaTicketRepository ticketRepository;
    @Mock private CorrespondenciaAreaRepository areaRepository;
    @Mock private CorrespondenciaDestinatarioRepository destinatarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AreaRepository areaCatRepository;
    @Mock private UsuarioPermisoRepository usuarioPermisoRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private TicketService ticketService;
    @Mock private NotificacionWebSocketService notificacionService;
    @Mock private AsignacionPuestoRepository asignacionPuestoRepository;
    @Mock private UnidadOrganizacionalRepository unidadOrganizacionalRepository;
    @Mock private AsignacionPuestoService asignacionPuestoService;
    @Mock private DelegacionFuncionService delegacionService;
    @Mock private CorrespondenciaVinculoRepository vinculoAsistenteRepository;

    private CorrespondenciaService service;

    private Usuario secretaria;
    private Usuario titular;
    private Puesto puestoAsistente;
    private Puesto puestoTitular;

    @BeforeEach
    void setUp() {
        service = new CorrespondenciaService(
            repository, tipoDocRepository, adjuntoRepository, historialRepository,
            respuestaRepository, ticketRepository, areaRepository, destinatarioRepository,
            usuarioRepository, areaCatRepository, usuarioPermisoRepository,
            auditoriaService, ticketService, notificacionService,
            asignacionPuestoRepository, unidadOrganizacionalRepository,
            asignacionPuestoService, delegacionService, vinculoAsistenteRepository);

        puestoAsistente = Puesto.builder().idPuesto(10).nombre("Secretaria").esJefatura(false).build();
        puestoTitular = Puesto.builder().idPuesto(20).nombre("Jefe TH").esJefatura(true).build();

        secretaria = Usuario.builder().idUsuario(100).username("maria.garcia")
            .nombres("María").apellidos("García").activo(true)
            .empleado(Empleado.builder().idEmpleado(300).build())
            .build();

        titular = Usuario.builder().idUsuario(200).username("juan.perez")
            .nombres("Juan").apellidos("Pérez").activo(true)
            .empleado(Empleado.builder().idEmpleado(400).build())
            .build();
    }

    private void givenSecretariaVinculada() {
        var asignacion = AsignacionPuesto.builder().idAsignacion(1)
            .empleado(secretaria.getEmpleado()).puesto(puestoAsistente)
            .esPrincipal(true).estado("ACTIVA")
            .fechaInicio(LocalDate.now().minusDays(30)).build();
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(secretaria));
        when(asignacionPuestoRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(300, "ACTIVA"))
            .thenReturn(Optional.of(asignacion));
        when(vinculoAsistenteRepository.findVigentesComoAsistente(10, LocalDate.now()))
            .thenReturn(List.of(CorrespondenciaVinculoAsistente.builder()
                .idVinculo(1)
                .puestoAsistente(puestoAsistente)
                .puestoTitular(puestoTitular)
                .puedeRecepcionar(true).puedeSeguimiento(true)
                .vigenteDesde(LocalDate.now().minusDays(10))
                .activo(true)
                .autorizadoPor(secretaria)
                .build()));
        // Usuarios que ocupan el puesto titular (se consulta tras descartar creador/responsable)
        lenient().when(usuarioRepository.findByPuestoVigente(20)).thenReturn(List.of(titular));
    }

    private Correspondencia documentoDelTitular() {
        return Correspondencia.builder()
            .idCorrespondencia(500)
            .numeroInterno("CORR-2025-001")
            .codigoDocumento("DOC-001")
            .tipoDocumento(CorrespondenciaDocumentoTipo.builder()
                .idTipoDocumento(1).nombre("Oficio").codigo("OFC").build())
            .asunto("Solicitud de personal")
            .estado("RECIBIDO").sentido("INGRESO").prioridad("MEDIA")
            .requiereRespuesta(false).generaTicket(false).activo(true)
            .creadoPor(titular)
            .build();
    }

    private void givenFlujoNormalPermitido(Correspondencia doc) {
        when(repository.findById(doc.getIdCorrespondencia())).thenReturn(Optional.of(doc));
        when(repository.save(any(Correspondencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(100)).thenReturn(secretaria);
        lenient().when(areaRepository.findByCorrespondenciaIdCorrespondencia(500)).thenReturn(List.of());
        lenient().when(repository.findReferenciadoPor(500)).thenReturn(List.of());
        lenient().when(destinatarioRepository.findByCorrespondenciaIdCorrespondencia(500)).thenReturn(List.of());
        lenient().when(adjuntoRepository.findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(500)).thenReturn(List.of());
        lenient().when(adjuntoRepository.countByCorrespondenciaIdCorrespondencia(500)).thenReturn(0L);
        lenient().when(historialRepository.findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnDesc(500))
            .thenReturn(List.of());
        lenient().when(respuestaRepository.findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(500))
            .thenReturn(List.of());
        lenient().when(repository.findTicketsByCorrespondenciaId(500)).thenReturn(List.of());
    }

    // ─── Test negativo principal: 403 sumilla ───

    @Test
    void cambiarEstado_secretariaVinculada_sobreDocumentoDelTitular_esProhibido() {
        givenSecretariaVinculada();
        when(repository.findById(500)).thenReturn(Optional.of(documentoDelTitular()));

        assertThatThrownBy(() -> service.cambiarEstado(500, "EN_ANALISIS", "avance", 100))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("no puede sumillar, derivar ni aprobar");
    }

    @Test
    void asignarResponsable_secretariaVinculada_sobreDocumentoDelTitular_esProhibido() {
        givenSecretariaVinculada();
        when(repository.findById(500)).thenReturn(Optional.of(documentoDelTitular()));

        assertThatThrownBy(() -> service.asignarResponsable(500, 200, "favor revisar", 100))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Bandeja Compartida");
    }

    @Test
    void recepcionarYDerivar_secretariaVinculada_sobreDocumentoDelTitular_esProhibido() {
        givenSecretariaVinculada();
        when(repository.findById(500)).thenReturn(Optional.of(documentoDelTitular()));

        assertThatThrownBy(() -> service.recepcionarYDerivar(500, "derivo", List.of(300), 100))
            .isInstanceOf(SecurityException.class);
    }

    // ─── La restricción está acotada: sin vínculo no aplica ───

    @Test
    void cambiarEstado_sinVinculo_asistente_noSeBloquea() {
        var asignacion = AsignacionPuesto.builder().idAsignacion(1)
            .empleado(secretaria.getEmpleado()).puesto(puestoAsistente)
            .esPrincipal(true).estado("ACTIVA")
            .fechaInicio(LocalDate.now().minusDays(30)).build();
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(secretaria));
        when(asignacionPuestoRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(300, "ACTIVA"))
            .thenReturn(Optional.of(asignacion));
        when(vinculoAsistenteRepository.findVigentesComoAsistente(10, LocalDate.now()))
            .thenReturn(List.of());

        var doc = documentoDelTitular();
        givenFlujoNormalPermitido(doc);

        var dto = service.cambiarEstado(500, "EN_ANALISIS", "avance", 100);

        assertThat(dto).isNotNull();
        assertThat(dto.estado()).isEqualTo("EN_ANALISIS");
    }

    // ─── La secretaria sí puede operar documentos de los que es creadora ───

    @Test
    void cambiarEstado_secretariaCreadoraDelDocumento_noSeBloquea() {
        givenSecretariaVinculada();
        var propio = documentoDelTitular();
        propio.setCreadoPor(secretaria);
        givenFlujoNormalPermitido(propio);

        var dto = service.cambiarEstado(500, "EN_TRAMITE", "gestión propia", 100);

        assertThat(dto).isNotNull();
        assertThat(dto.estado()).isEqualTo("EN_TRAMITE");
    }

    // ─── Sin asignación de puesto: sin restricción ───

    @Test
    void cambiarEstado_sinAsignacionVigente_noSeBloqueaPorBcc() {
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(secretaria));
        when(asignacionPuestoRepository
            .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(300, "ACTIVA"))
            .thenReturn(Optional.empty());

        var doc = documentoDelTitular();
        givenFlujoNormalPermitido(doc);

        var dto = service.cambiarEstado(500, "EN_ANALISIS", "sin puesto", 100);

        assertThat(dto).isNotNull();
    }
}
