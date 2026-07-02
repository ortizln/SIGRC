package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.service.TicketService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "Gestión de tickets (incidentes, requerimientos, cambios, etc.)")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "Listar tickets", description = "Lista paginada de tickets con filtros")
    public ResponseEntity<PaginacionDTO<TicketDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) Integer idSolicitante,
            @RequestParam(required = false) Integer idResponsable,
            @RequestParam(required = false) Integer idArea,
            @RequestParam(required = false) Integer idSistema,
            @RequestParam(required = false) String texto,
            Authentication auth) {
        var user = (UserPrincipal) auth.getPrincipal();
        Integer idUsuario = "ADMIN".equals(user.rol()) ? null : user.idUsuario();
        return ResponseEntity.ok(ticketService.listar(pagina, tamanio, estado, tipo,
            prioridad, idSolicitante, idResponsable, idArea, idSistema, idUsuario, texto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ticket por ID")
    public ResponseEntity<TicketDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI','TECNICO','SOLICITANTE')")
    @Operation(summary = "Crear ticket")
    public ResponseEntity<TicketDTO> crear(@Valid @RequestBody TicketCrearRequest request) {
        return ResponseEntity.ok(ticketService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI','TECNICO')")
    @Operation(summary = "Actualizar ticket")
    public ResponseEntity<TicketDTO> actualizar(@PathVariable Integer id,
                                                  @Valid @RequestBody TicketCrearRequest request) {
        return ResponseEntity.ok(ticketService.actualizarTicket(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Eliminar ticket (desactivar)")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        ticketService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI','TECNICO')")
    @Operation(summary = "Actualizar estado de ticket")
    public ResponseEntity<TicketDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado,
            @RequestParam Integer idUsuario,
            @RequestParam(required = false) String observacion) {
        return ResponseEntity.ok(ticketService.actualizarEstado(id, estado, idUsuario, observacion));
    }

    @PatchMapping("/{id}/asignar")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Asignar responsable a ticket")
    public ResponseEntity<TicketDTO> asignar(
            @PathVariable Integer id,
            @RequestParam Integer idResponsable,
            @RequestParam Integer idUsuario) {
        return ResponseEntity.ok(ticketService.asignarResponsable(id, idResponsable, idUsuario));
    }

    @GetMapping("/{id}/comentarios")
    @Operation(summary = "Obtener comentarios de ticket")
    public ResponseEntity<?> comentarios(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.obtenerComentarios(id));
    }

    @PostMapping("/{id}/comentarios")
    @Operation(summary = "Agregar comentario a ticket")
    public ResponseEntity<?> agregarComentario(
            @PathVariable Integer id,
            @RequestParam Integer idUsuario,
            @RequestParam String comentario,
            @RequestParam(defaultValue = "false") boolean esInterno) {
        return ResponseEntity.ok(ticketService.agregarComentario(id, idUsuario, comentario, esInterno));
    }
}
