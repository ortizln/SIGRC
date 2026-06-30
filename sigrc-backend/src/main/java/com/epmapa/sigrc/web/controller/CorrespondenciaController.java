package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.entity.CorrespondenciaDocumentoTipo;
import com.epmapa.sigrc.domain.service.CorrespondenciaService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/correspondencia")
@Tag(name = "Correspondencia", description = "Gestión de Correspondencia Institucional")
public class CorrespondenciaController {

    private final CorrespondenciaService service;

    public CorrespondenciaController(CorrespondenciaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar documentos con filtros y paginación")
    public ResponseEntity<PaginacionDTO<CorrespondenciaDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) Integer idTipoDocumento,
            @RequestParam(required = false) Integer idResponsable,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(service.listar(texto, estado, prioridad, idTipoDocumento,
                idResponsable, fechaDesde, fechaHasta, pagina, tamanio));
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
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI','SUPERVISOR')")
    @Operation(summary = "Asignar responsable al documento")
    public ResponseEntity<CorrespondenciaDTO> asignar(@PathVariable Integer id,
                                                       @RequestParam Integer idResponsable,
                                                       Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.asignarResponsable(id, idResponsable, idUsuario));
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
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
