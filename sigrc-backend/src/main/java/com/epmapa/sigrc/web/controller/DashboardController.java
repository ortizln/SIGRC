package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.DashboardDTO;
import com.epmapa.sigrc.domain.dto.DocumentoResumenDTO;
import com.epmapa.sigrc.domain.dto.TicketResumenDTO;
import com.epmapa.sigrc.domain.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    @Operation(summary = "Obtener dashboard completo")
    public ResponseEntity<DashboardDTO> dashboard() {
        return ResponseEntity.ok(dashboardService.obtenerDashboard());
    }

    @GetMapping("/tickets/abiertos")
    @Operation(summary = "Lista de tickets abiertos")
    public ResponseEntity<List<TicketResumenDTO>> ticketsAbiertos() {
        return ResponseEntity.ok(dashboardService.listarTicketsAbiertos());
    }

    @GetMapping("/tickets/cerrados")
    @Operation(summary = "Lista de tickets cerrados")
    public ResponseEntity<List<TicketResumenDTO>> ticketsCerrados() {
        return ResponseEntity.ok(dashboardService.listarTicketsCerrados());
    }

    @GetMapping("/tickets/vencidos")
    @Operation(summary = "Lista de tickets vencidos")
    public ResponseEntity<List<TicketResumenDTO>> ticketsVencidos() {
        return ResponseEntity.ok(dashboardService.listarTicketsVencidos());
    }

    @GetMapping("/tickets/sin-asignar")
    @Operation(summary = "Lista de tickets sin asignar")
    public ResponseEntity<List<TicketResumenDTO>> ticketsSinAsignar() {
        return ResponseEntity.ok(dashboardService.listarTicketsSinAsignar());
    }

    @GetMapping("/documentos/pendientes")
    @Operation(summary = "Lista de documentos pendientes de respuesta")
    public ResponseEntity<List<DocumentoResumenDTO>> documentosPendientes() {
        return ResponseEntity.ok(dashboardService.listarDocumentosPendientes());
    }

    @GetMapping("/documentos/vencidos")
    @Operation(summary = "Lista de documentos vencidos")
    public ResponseEntity<List<DocumentoResumenDTO>> documentosVencidos() {
        return ResponseEntity.ok(dashboardService.listarDocumentosVencidos());
    }

    @GetMapping("/documentos/con-ticket")
    @Operation(summary = "Lista de documentos que generaron ticket")
    public ResponseEntity<List<DocumentoResumenDTO>> documentosConTicket() {
        return ResponseEntity.ok(dashboardService.listarDocumentosConTicket());
    }
}
