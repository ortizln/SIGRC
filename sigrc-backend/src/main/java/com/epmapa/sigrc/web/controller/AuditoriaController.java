package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.AuditoriaDTO;
import com.epmapa.sigrc.domain.dto.PaginacionDTO;
import com.epmapa.sigrc.domain.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auditoria")
@Tag(name = "Auditoría", description = "Consulta de bitácora de auditoría general")
@PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar registros de auditoría")
    public ResponseEntity<PaginacionDTO<AuditoriaDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String tipoOperacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        if (desde == null) desde = LocalDateTime.now().minusMonths(1);
        if (hasta == null) hasta = LocalDateTime.now();
        return ResponseEntity.ok(auditoriaService.listar(pagina, tamanio, username, tabla, tipoOperacion, desde, hasta));
    }
}
