package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.service.GestionPersonalService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/talento-humano")
@Tag(name = "Gestión de Personal", description = "Movimientos, acciones de personal, vacaciones, permisos y licencias")
public class GestionPersonalController {

    private final GestionPersonalService service;

    public GestionPersonalController(GestionPersonalService service) {
        this.service = service;
    }

    private Integer idUsuario(Authentication auth) {
        return ((UserPrincipal) auth.getPrincipal()).idUsuario();
    }

    // ─────────── Movimientos de personal ───────────

    @GetMapping("/movimientos")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar movimientos de personal (por empleado y/o estado)")
    public ResponseEntity<List<MovimientoPersonalDTO>> listarMovimientos(
            @RequestParam(required = false) Integer idEmpleado,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarMovimientos(idEmpleado, estado));
    }

    @PostMapping("/movimientos")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear movimiento de personal")
    public ResponseEntity<MovimientoPersonalDTO> crearMovimiento(@RequestBody MovimientoPersonalRequest req,
                                                                  Authentication auth) {
        return ResponseEntity.ok(service.crearMovimiento(req, idUsuario(auth)));
    }

    @PutMapping("/movimientos/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar movimiento de personal")
    public ResponseEntity<MovimientoPersonalDTO> actualizarMovimiento(@PathVariable Integer id,
                                                                      @RequestBody MovimientoPersonalRequest req,
                                                                      Authentication auth) {
        return ResponseEntity.ok(service.actualizarMovimiento(id, req, idUsuario(auth)));
    }

    @PostMapping("/movimientos/{id}/enviar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enviar movimiento a aprobación")
    public ResponseEntity<MovimientoPersonalDTO> enviarMovimiento(@PathVariable Integer id,
                                                                  Authentication auth) {
        return ResponseEntity.ok(service.enviarMovimiento(id, idUsuario(auth)));
    }

    @PostMapping("/movimientos/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar movimiento de personal")
    public ResponseEntity<MovimientoPersonalDTO> aprobarMovimiento(@PathVariable Integer id,
                                                                   Authentication auth) {
        return ResponseEntity.ok(service.aprobarMovimiento(id, idUsuario(auth)));
    }

    @PostMapping("/movimientos/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar movimiento de personal")
    public ResponseEntity<MovimientoPersonalDTO> rechazarMovimiento(@PathVariable Integer id,
                                                                    Authentication auth) {
        return ResponseEntity.ok(service.rechazarMovimiento(id, idUsuario(auth)));
    }

    @PostMapping("/movimientos/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anular movimiento de personal")
    public ResponseEntity<MovimientoPersonalDTO> anularMovimiento(@PathVariable Integer id,
                                                                  Authentication auth) {
        return ResponseEntity.ok(service.anularMovimiento(id, idUsuario(auth)));
    }

    @PostMapping("/movimientos/{id}/ejecutar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ejecutar movimiento (registra la nueva asignación de puesto)")
    public ResponseEntity<MovimientoPersonalDTO> ejecutarMovimiento(@PathVariable Integer id,
                                                                    Authentication auth) {
        return ResponseEntity.ok(service.ejecutarMovimiento(id, idUsuario(auth)));
    }

    // ─────────── Acciones de personal ───────────

    @GetMapping("/acciones-personal")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar acciones de personal")
    public ResponseEntity<List<AccionPersonalDTO>> listarAcciones(
            @RequestParam(required = false) Integer idEmpleado,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarAcciones(idEmpleado, estado));
    }

    @PostMapping("/acciones-personal")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear acción de personal")
    public ResponseEntity<AccionPersonalDTO> crearAccion(@RequestBody AccionPersonalRequest req,
                                                         Authentication auth) {
        return ResponseEntity.ok(service.crearAccion(req, idUsuario(auth)));
    }

    @PutMapping("/acciones-personal/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar acción de personal")
    public ResponseEntity<AccionPersonalDTO> actualizarAccion(@PathVariable Integer id,
                                                              @RequestBody AccionPersonalRequest req,
                                                              Authentication auth) {
        return ResponseEntity.ok(service.actualizarAccion(id, req, idUsuario(auth)));
    }

    @PostMapping("/acciones-personal/{id}/enviar-revision")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enviar acción de personal a revisión")
    public ResponseEntity<AccionPersonalDTO> enviarRevisionAccion(@PathVariable Integer id,
                                                                  Authentication auth) {
        return ResponseEntity.ok(service.enviarRevisionAccion(id, idUsuario(auth)));
    }

    @PostMapping("/acciones-personal/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar acción de personal")
    public ResponseEntity<AccionPersonalDTO> aprobarAccion(@PathVariable Integer id,
                                                           Authentication auth) {
        return ResponseEntity.ok(service.aprobarAccion(id, idUsuario(auth)));
    }

    @PostMapping("/acciones-personal/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar acción de personal")
    public ResponseEntity<AccionPersonalDTO> rechazarAccion(@PathVariable Integer id,
                                                            Authentication auth) {
        return ResponseEntity.ok(service.rechazarAccion(id, idUsuario(auth)));
    }

    @PostMapping("/acciones-personal/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anular acción de personal")
    public ResponseEntity<AccionPersonalDTO> anularAccion(@PathVariable Integer id,
                                                          Authentication auth) {
        return ResponseEntity.ok(service.anularAccion(id, idUsuario(auth)));
    }

    // ─────────── Vacaciones, permisos y licencias ───────────

    @GetMapping("/ausencias")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar solicitudes de ausencia (vacaciones, permisos, licencias)")
    public ResponseEntity<List<SolicitudAusenciaDTO>> listarAusencias(
            @RequestParam(required = false) Integer idEmpleado,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarAusencias(idEmpleado, estado));
    }

    @PostMapping("/ausencias")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear solicitud de ausencia (va al jefe inmediato)")
    public ResponseEntity<SolicitudAusenciaDTO> crearAusencia(@RequestBody SolicitudAusenciaRequest req,
                                                              Authentication auth) {
        return ResponseEntity.ok(service.crearAusencia(req, idUsuario(auth)));
    }

    @PostMapping("/ausencias/{id}/aprobar-jefe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aprobar solicitud como jefe inmediato (verificado por estructura)")
    public ResponseEntity<SolicitudAusenciaDTO> aprobarJefe(@PathVariable Integer id,
                                                            Authentication auth) {
        return ResponseEntity.ok(service.aprobarAusenciaJefe(id, idUsuario(auth)));
    }

    @PostMapping("/ausencias/{id}/aprobar-th")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar solicitud como Talento Humano")
    public ResponseEntity<SolicitudAusenciaDTO> aprobarTh(@PathVariable Integer id,
                                                          Authentication auth) {
        return ResponseEntity.ok(service.aprobarAusenciaTh(id, idUsuario(auth)));
    }

    @PostMapping("/ausencias/{id}/rechazar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rechazar solicitud de ausencia")
    public ResponseEntity<SolicitudAusenciaDTO> rechazarAusencia(@PathVariable Integer id,
                                                                 Authentication auth) {
        return ResponseEntity.ok(service.rechazarAusencia(id, idUsuario(auth)));
    }

    @PostMapping("/ausencias/{id}/anular")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Anular solicitud de ausencia")
    public ResponseEntity<SolicitudAusenciaDTO> anularAusencia(@PathVariable Integer id,
                                                               Authentication auth) {
        return ResponseEntity.ok(service.anularAusencia(id, idUsuario(auth)));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}