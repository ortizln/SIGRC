package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.VersionDTO;
import com.epmapa.sigrc.domain.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/versiones")
@Tag(name = "Versiones", description = "Gestión de versiones de sistemas")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    @Operation(summary = "Listar versiones")
    public ResponseEntity<List<VersionDTO>> listar() {
        return ResponseEntity.ok(versionService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener versión por ID")
    public ResponseEntity<VersionDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(versionService.obtenerPorId(id));
    }

    @GetMapping("/sistema/{idSistema}")
    @Operation(summary = "Listar versiones por sistema")
    public ResponseEntity<List<VersionDTO>> listarPorSistema(@PathVariable Integer idSistema) {
        return ResponseEntity.ok(versionService.listarPorSistema(idSistema));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Crear versión")
    public ResponseEntity<VersionDTO> crear(@RequestBody VersionDTO dto) {
        return ResponseEntity.ok(versionService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Actualizar versión")
    public ResponseEntity<VersionDTO> actualizar(@PathVariable Integer id, @RequestBody VersionDTO dto) {
        return ResponseEntity.ok(versionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Desactivar versión")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        versionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
