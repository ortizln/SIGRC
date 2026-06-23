package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.CambioDTO;
import com.epmapa.sigrc.domain.service.CambioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cambios")
@Tag(name = "Cambios", description = "Gestión de cambios del sistema")
public class CambioController {

    private final CambioService cambioService;

    public CambioController(CambioService cambioService) {
        this.cambioService = cambioService;
    }

    @GetMapping
    @Operation(summary = "Listar cambios")
    public ResponseEntity<List<CambioDTO>> listar() {
        return ResponseEntity.ok(cambioService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cambio por ID")
    public ResponseEntity<CambioDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(cambioService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Crear solicitud de cambio")
    public ResponseEntity<CambioDTO> crear(@RequestBody CambioDTO dto) {
        return ResponseEntity.ok(cambioService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Actualizar cambio")
    public ResponseEntity<CambioDTO> actualizar(@PathVariable Integer id, @RequestBody CambioDTO dto) {
        return ResponseEntity.ok(cambioService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Eliminar cambio (desactivar)")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        cambioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_TI')")
    @Operation(summary = "Aprobar cambio")
    public ResponseEntity<CambioDTO> aprobar(@PathVariable Integer id, @RequestParam Integer idAprobador) {
        return ResponseEntity.ok(cambioService.aprobar(id, idAprobador));
    }
}
