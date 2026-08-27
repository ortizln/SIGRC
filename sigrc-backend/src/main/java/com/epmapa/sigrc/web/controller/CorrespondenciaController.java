package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.CorrespondenciaDocumentoTipo;
import com.epmapa.sigrc.domain.service.CorrespondenciaService;
import com.epmapa.sigrc.domain.service.DelegacionFuncionService;
import com.epmapa.sigrc.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/correspondencia")
@Tag(name = "Correspondencia", description = "Gestión de Correspondencia Institucional")
public class CorrespondenciaController {

    private final CorrespondenciaService service;
    private final DelegacionFuncionService delegacionService;

    public CorrespondenciaController(CorrespondenciaService service, DelegacionFuncionService delegacionService) {
        this.service = service;
        this.delegacionService = delegacionService;
    }

    @GetMapping
    @Operation(summary = "Listar documentos con filtros y paginación")
    public ResponseEntity<PaginacionDTO<CorrespondenciaDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) Integer idTipoDocumento,
            @RequestParam(required = false) Integer idResponsable,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) String sentido,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(defaultValue = "creado_en") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(service.listar(texto, estado, prioridad, idTipoDocumento,
                idResponsable, idUsuario, sentido, fechaDesde, fechaHasta, pagina, tamanio, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener documento por ID")
    public ResponseEntity<CorrespondenciaDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar nueva correspondencia")
    public ResponseEntity<CorrespondenciaDTO> crear(@Valid @RequestBody CorrespondenciaCrearRequest request,
                                                     Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.crear(request, idUsuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar correspondencia")
    public ResponseEntity<CorrespondenciaDTO> actualizar(@PathVariable Integer id,
                                                          @Valid @RequestBody CorrespondenciaActualizarRequest request,
                                                          Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.actualizar(id, request, idUsuario));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar estado del documento")
    public ResponseEntity<CorrespondenciaDTO> cambiarEstado(@PathVariable Integer id,
                                                             @RequestParam String estado,
                                                             @RequestParam(required = false) String detalle,
                                                             Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.cambiarEstado(id, estado, detalle, idUsuario));
    }

    @PatchMapping("/{id}/asignar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Asignar responsable al documento")
    public ResponseEntity<CorrespondenciaDTO> asignar(@PathVariable Integer id,
                                                       @RequestParam Integer idResponsable,
                                                       @RequestParam(required = false) String sumilla,
                                                       Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.asignarResponsable(id, idResponsable, sumilla, idUsuario));
    }

    @PostMapping("/{id}/recibir")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar como recibido por el destinatario")
    public ResponseEntity<CorrespondenciaDTO> marcarRecibido(@PathVariable Integer id,
                                                              Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.marcarRecibido(id, idUsuario));
    }

    @PostMapping("/{id}/leido")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar el documento como leído por el destinatario")
    public ResponseEntity<CorrespondenciaDTO> marcarLeido(@PathVariable Integer id,
                                                           Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.marcarLeido(id, idUsuario));
    }

    @PostMapping("/{id}/recepcion")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Recepcionar con sumilla y derivar/etiquetar a otros usuarios")
    public ResponseEntity<CorrespondenciaDTO> recepcionar(@PathVariable Integer id,
                                                            @RequestBody(required = false) CorrespondenciaRecepcionRequest request,
                                                            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        String sumilla = request != null ? request.sumilla() : null;
        List<Integer> idsDerivados = request != null ? request.idsUsuariosDerivados() : null;
        return ResponseEntity.ok(service.recepcionarYDerivar(id, sumilla, idsDerivados, idUsuario));
    }

    @PostMapping("/{id}/derivar-institucional")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Derivar por destino institucional (USUARIO, PUESTO, UNIDAD, RESPONSABLE_UNIDAD, JEFE_INMEDIATO)")
    public ResponseEntity<CorrespondenciaDTO> derivarInstitucional(@PathVariable Integer id,
                                                                     @RequestBody CorrespondenciaDerivarRequest request,
                                                                     Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.derivarInstitucional(id, request.sumilla(), request.destinos(), idUsuario));
    }

    @GetMapping("/bandeja-unidad")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Documentos de mi unidad (bandeja por unidad organizacional)")
    public ResponseEntity<List<CorrespondenciaDTO>> bandejaUnidad(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.bandejaUnidad(idUsuario));
    }

    @GetMapping("/bandeja-puesto")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Documentos de mi puesto (bandeja por puesto institucional)")
    public ResponseEntity<List<CorrespondenciaDTO>> bandejaPuesto(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.bandejaPuesto(idUsuario));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Documentos pendientes de atención del usuario")
    public ResponseEntity<List<CorrespondenciaDTO>> pendientes(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.pendientes(idUsuario));
    }

    @PostMapping("/{id}/respuesta")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar respuesta al documento")
    public ResponseEntity<CorrespondenciaRespuestaDTO> registrarRespuesta(
            @PathVariable Integer id,
            @Valid @RequestBody CorrespondenciaRespuestaRequest request,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.registrarRespuesta(
                new CorrespondenciaRespuestaRequest(id, request.fechaRespuesta(),
                        request.numeroDocumento(), request.idTipoDocumento(),
                        request.idResponsable(), request.observaciones()),
                idUsuario));
    }

    @GetMapping("/{id}/adjuntos")
    @Operation(summary = "Listar adjuntos del documento")
    public ResponseEntity<List<CorrespondenciaAdjuntoDTO>> listarAdjuntos(@PathVariable Integer id) {
        return ResponseEntity.ok(service.listarAdjuntos(id));
    }

    @PostMapping("/{id}/adjuntos")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subir adjunto al documento")
    public ResponseEntity<CorrespondenciaAdjuntoDTO> subirAdjunto(@PathVariable Integer id,
                                                                   @RequestParam("file") MultipartFile file,
                                                                   @RequestParam(defaultValue = "ANEXO") String tipo,
                                                                   Authentication auth) throws IOException {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.subirAdjunto(id, file, tipo, idUsuario));
    }

    @GetMapping("/{id}/adjuntos/{idAdjunto}/descargar")
    @Operation(summary = "Descargar archivo adjunto")
    public ResponseEntity<Resource> descargarAdjunto(@PathVariable Integer id,
                                                      @PathVariable Integer idAdjunto) throws IOException {
        Path path = service.getAdjuntoPath(idAdjunto);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Archivo no encontrado en disco");
        }
        var adjunto = service.obtenerAdjunto(idAdjunto);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(adjunto.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + adjunto.nombreOriginal() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/adjuntos/{idAdjunto}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Eliminar adjunto")
    public ResponseEntity<Void> eliminarAdjunto(@PathVariable Integer id,
                                                 @PathVariable Integer idAdjunto,
                                                 Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        service.eliminarAdjunto(idAdjunto, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Obtener historial de cambios")
    public ResponseEntity<List<CorrespondenciaHistorialDTO>> obtenerHistorial(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtenerHistorial(id));
    }

    @GetMapping("/{id}/tickets")
    @Operation(summary = "Obtener tickets vinculados al documento")
    public ResponseEntity<List<TicketVinculadoDTO>> obtenerTickets(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtenerTicketsVinculados(id));
    }

    @PostMapping("/{id}/generar-ticket")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generar ticket desde el documento")
    public ResponseEntity<TicketVinculadoDTO> generarTicket(@PathVariable Integer id,
                                                             Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.generarTicketDesdeCorrespondencia(id, idUsuario));
    }

    @PostMapping("/{id}/vincular-ticket")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vincular ticket existente al documento")
    public ResponseEntity<TicketVinculadoDTO> vincularTicket(@PathVariable Integer id,
                                                              @RequestParam Integer idTicket,
                                                              Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.vincularTicketExistente(id, idTicket, idUsuario));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI','AUDITOR','SUPERVISOR')")
    @Operation(summary = "Dashboard de indicadores documentales")
    public ResponseEntity<CorrespondenciaDashboardDTO> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/tipos-documento")
    @Operation(summary = "Listar tipos de documento")
    public ResponseEntity<List<CorrespondenciaDocumentoTipo>> listarTiposDocumento() {
        return ResponseEntity.ok(service.listarTiposDocumento());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Anular documento (soft-delete)")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, Authentication auth,
                                          HttpServletRequest request) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        service.eliminar(id, idUsuario, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/delegaciones-vigentes")
    @Operation(summary = "Delegaciones vigentes para el usuario autenticado")
    public ResponseEntity<List<DelegacionResueltaDTO>> delegacionesVigentes(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        var delegacion = delegacionService.resolverDelegadoConDetalle(idUsuario);
        var delegaron = delegacionService.usuariosQueMeDelegaron(idUsuario);
        var resultado = new java.util.ArrayList<DelegacionResueltaDTO>();
        if (delegacion != null) {
            resultado.add(delegacion);
        }
        for (Integer idOrig : delegaron) {
            var det = delegacionService.resolverDelegadoConDetalle(idOrig);
            if (det != null) {
                resultado.add(new DelegacionResueltaDTO(
                    idUsuario,
                    det.idDelegacion(),
                    idOrig,
                    det.nombreDelegado(),
                    det.nombreOriginal(),
                    det.tipoDelegacion(),
                    det.fechaInicio(),
                    det.fechaFin()
                ));
            }
        }
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/mis-delegaciones")
    @Operation(summary = "IDs de usuarios que delegaron al usuario autenticado")
    public ResponseEntity<List<Integer>> misDelegaciones(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(delegacionService.usuariosQueMeDelegaron(idUsuario));
    }

    @GetMapping("/documentos-por-delegacion")
    @Operation(summary = "Cantidad de documentos procesados por cada delegación")
    public ResponseEntity<Map<Integer, Long>> documentosPorDelegacion() {
        return ResponseEntity.ok(service.contarDocumentosPorDelegacion());
    }
}
