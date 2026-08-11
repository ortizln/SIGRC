package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.DashboardDTO;
import com.epmapa.sigrc.domain.dto.DocumentoResumenDTO;
import com.epmapa.sigrc.domain.dto.TicketResumenDTO;
import com.epmapa.sigrc.domain.service.DashboardService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Indicadores y estadísticas del sistema")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    private int idUsuario(Authentication auth) {
        return ((UserPrincipal) auth.getPrincipal()).idUsuario();
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
    }

    @GetMapping
    @Operation(summary = "Obtener dashboard completo")
    public ResponseEntity<DashboardDTO> dashboard(Authentication auth) {
        return ResponseEntity.ok(dashboardService.obtenerDashboard(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/tickets/abiertos")
    @Operation(summary = "Lista de tickets abiertos")
    public ResponseEntity<List<TicketResumenDTO>> ticketsAbiertos(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarTicketsAbiertos(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/tickets/cerrados")
    @Operation(summary = "Lista de tickets cerrados")
    public ResponseEntity<List<TicketResumenDTO>> ticketsCerrados(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarTicketsCerrados(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/tickets/vencidos")
    @Operation(summary = "Lista de tickets vencidos")
    public ResponseEntity<List<TicketResumenDTO>> ticketsVencidos(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarTicketsVencidos(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/tickets/sin-asignar")
    @Operation(summary = "Lista de tickets sin asignar")
    public ResponseEntity<List<TicketResumenDTO>> ticketsSinAsignar(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarTicketsSinAsignar(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/documentos/pendientes")
    @Operation(summary = "Lista de documentos pendientes de respuesta")
    public ResponseEntity<List<DocumentoResumenDTO>> documentosPendientes(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarDocumentosPendientes(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/documentos/vencidos")
    @Operation(summary = "Lista de documentos vencidos")
    public ResponseEntity<List<DocumentoResumenDTO>> documentosVencidos(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarDocumentosVencidos(idUsuario(auth), esAdmin(auth)));
    }

    @GetMapping("/documentos/con-ticket")
    @Operation(summary = "Lista de documentos que generaron ticket")
    public ResponseEntity<List<DocumentoResumenDTO>> documentosConTicket(Authentication auth) {
        return ResponseEntity.ok(dashboardService.listarDocumentosConTicket(idUsuario(auth), esAdmin(auth)));
    }
}
