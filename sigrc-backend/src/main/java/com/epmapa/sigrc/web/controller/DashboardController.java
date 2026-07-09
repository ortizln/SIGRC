package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.DashboardDTO;
import com.epmapa.sigrc.domain.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
