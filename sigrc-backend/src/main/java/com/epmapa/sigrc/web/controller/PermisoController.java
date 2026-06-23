package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.PermisoDTO;
import com.epmapa.sigrc.domain.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permisos")
@Tag(name = "Permisos", description = "Gestión de permisos del sistema")
public class PermisoController {

    private final PermisoService permisoService;

    public PermisoController(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @GetMapping
    @Operation(summary = "Listar permisos")
    public ResponseEntity<List<PermisoDTO>> listar() {
        return ResponseEntity.ok(permisoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener permiso por ID")
    public ResponseEntity<PermisoDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(permisoService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear permiso")
    public ResponseEntity<PermisoDTO> crear(@RequestBody PermisoDTO dto) {
        return ResponseEntity.ok(permisoService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar permiso")
    public ResponseEntity<PermisoDTO> actualizar(@PathVariable Integer id, @RequestBody PermisoDTO dto) {
        return ResponseEntity.ok(permisoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar permiso")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        permisoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
