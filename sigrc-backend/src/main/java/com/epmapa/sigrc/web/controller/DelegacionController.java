package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.DelegacionFuncionDTO;
import com.epmapa.sigrc.domain.dto.DelegacionFuncionRequest;
import com.epmapa.sigrc.domain.service.DelegacionFuncionService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/talento-humano/delegaciones")
@Tag(name = "Delegaciones", description = "Delegación de funciones para cubrir vacaciones, encargos y ausencias")
public class DelegacionController {

    private final DelegacionFuncionService service;

    public DelegacionController(DelegacionFuncionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar delegaciones de funciones")
    public ResponseEntity<List<DelegacionFuncionDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear delegación de funciones")
    public ResponseEntity<DelegacionFuncionDTO> crear(@RequestBody DelegacionFuncionRequest req,
                                                      Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(service.crear(req, idUsuario));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancelar delegación de funciones")
    public ResponseEntity<DelegacionFuncionDTO> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Finalizar delegación de funciones")
    public ResponseEntity<DelegacionFuncionDTO> finalizar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.finalizar(id));
    }
}