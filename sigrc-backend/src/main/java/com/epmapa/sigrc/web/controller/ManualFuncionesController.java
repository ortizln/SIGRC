package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.ManualFuncionesDTO;
import com.epmapa.sigrc.domain.dto.VersionManualDTO;
import com.epmapa.sigrc.domain.dto.VersionManualRequest;
import com.epmapa.sigrc.domain.service.ManualFuncionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/talento-humano/manual-funciones")
@Tag(name = "Manual de Funciones", description = "Manual de funciones digital y control de versiones del manual")
public class ManualFuncionesController {

    private final ManualFuncionesService service;

    public ManualFuncionesController(ManualFuncionesService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener el manual de funciones estructurado (dirección → unidad → puestos con ficha)")
    public ResponseEntity<ManualFuncionesDTO> manualFunciones() {
        return ResponseEntity.ok(service.estructuraManual());
    }

    @GetMapping("/versiones")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar versiones del manual")
    public ResponseEntity<List<VersionManualDTO>> listarVersiones() {
        return ResponseEntity.ok(service.listarVersiones());
    }

    @PostMapping("/versiones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear versión del manual (BORRADOR)")
    public ResponseEntity<VersionManualDTO> crearVersion(@RequestBody VersionManualRequest req) {
        return ResponseEntity.ok(service.crearVersion(req));
    }

    @PutMapping("/versiones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar versión del manual")
    public ResponseEntity<VersionManualDTO> actualizarVersion(@PathVariable Integer id,
                                                              @RequestBody VersionManualRequest req) {
        return ResponseEntity.ok(service.actualizarVersion(id, req));
    }

    @PostMapping("/versiones/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar versión del manual (queda VIGENTE y deroga la anterior)")
    public ResponseEntity<VersionManualDTO> aprobarVersion(@PathVariable Integer id) {
        return ResponseEntity.ok(service.aprobarVersion(id));
    }

    @PostMapping("/versiones/{id}/derogar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Derogar versión del manual")
    public ResponseEntity<VersionManualDTO> derogarVersion(@PathVariable Integer id) {
        return ResponseEntity.ok(service.derogarVersion(id));
    }
}