package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.*;
import com.epmapa.sigrc.domain.service.BandejaCompartidaService;
import com.epmapa.sigrc.domain.service.CorrespondenciaService;
import com.epmapa.sigrc.domain.service.RecepcionDelegadaService;
import com.epmapa.sigrc.domain.service.VinculoAsistenteService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BCC — Ampliación: Bandeja Compartida Controlada.
 * Extiende la gestión de correspondencia con recepción delegada
 * por asistentes y bandejas compartidas hacia titulares.
 */
@RestController
@RequestMapping("/correspondencia")
@Tag(name = "Bandeja Compartida", description = "Recepción delegada y bandejas compartidas asistente→titular")
public class BandejaCompartidaController {

    private final VinculoAsistenteService vinculoService;
    private final RecepcionDelegadaService recepcionService;
    private final BandejaCompartidaService bandejaService;
    private final CorrespondenciaService correspondenciaService;

    public BandejaCompartidaController(VinculoAsistenteService vinculoService,
                                       RecepcionDelegadaService recepcionService,
                                       BandejaCompartidaService bandejaService,
                                       CorrespondenciaService correspondenciaService) {
        this.vinculoService = vinculoService;
        this.recepcionService = recepcionService;
        this.bandejaService = bandejaService;
        this.correspondenciaService = correspondenciaService;
    }

    // ─── Vínculos asistente→titular (solo ADMIN) ───

    @GetMapping("/vinculos-asistente")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los vínculos asistente→titular")
    public ResponseEntity<List<VinculoAsistenteDTO>> listarVinculos() {
        return ResponseEntity.ok(vinculoService.listarTodos());
    }

    @PostMapping("/vinculos-asistente")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear vínculo asistente→titular")
    public ResponseEntity<VinculoAsistenteDTO> crearVinculo(
            @Valid @RequestBody VinculoAsistenteRequest request,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(vinculoService.crear(request, idUsuario));
    }

    @PatchMapping("/vinculos-asistente/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar vínculo asistente→titular")
    public ResponseEntity<VinculoAsistenteDTO> actualizarVinculo(
            @PathVariable Integer id,
            @Valid @RequestBody VinculoAsistenteRequest request) {
        return ResponseEntity.ok(vinculoService.actualizar(id, request));
    }

    @PostMapping("/vinculos-asistente/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar vínculo asistente→titular")
    public ResponseEntity<Void> desactivarVinculo(@PathVariable Integer id) {
        vinculoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Mis bandejas compartidas ───

    @GetMapping("/mis-bandejas-compartidas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bandejas compartidas disponibles para el asistente autenticado")
    public ResponseEntity<List<BandejaCompartidaDTO>> misBandejasCompartidas(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(bandejaService.listarMisBandejas(idUsuario));
    }

    @GetMapping("/bandeja-compartida/{puestoId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Documentos de la bandeja de un puesto titular")
    public ResponseEntity<List<CorrespondenciaDTO>> bandejaCompartida(
            @PathVariable Integer puestoId,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(bandejaService.bandejaTitular(puestoId, idUsuario));
    }

    // ─── Recepción delegada ───

    @PostMapping("/recepcion-delegada")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar recepción delegada por asistente",
               description = "Recepción sobre bandeja del titular sin suplantación")
    public ResponseEntity<CorrespondenciaDTO> recepcionDelegada(
            @Valid @RequestBody RecepcionDelegadaRequest request,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        var documento = recepcionService.recepcionar(request, idUsuario);
        return ResponseEntity.ok(correspondenciaService.obtener(documento.getIdCorrespondencia()));
    }

    // ─── Lectura personal ───

    @PutMapping("/{id}/lectura-personal")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar documento como leído por este usuario (no global)")
    public ResponseEntity<Void> marcarLeidoPersonal(
            @PathVariable Integer id,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        bandejaService.marcarLeidoPersonal(id, idUsuario);
        return ResponseEntity.noContent().build();
    }

    // ─── Verificación de vínculo para el frontend ───

    @GetMapping("/lecturas-personales")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "IDs de documentos ya leídos por el usuario autenticado")
    public ResponseEntity<List<Integer>> lecturasPersonales(
            @RequestParam List<Integer> ids,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(bandejaService.lecturasDe(ids, idUsuario));
    }

    @GetMapping("/verificar-vinculo/{puestoTitularId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verificar si el usuario autenticado tiene vínculo vigente con un puesto titular")
    public ResponseEntity<Map<String, Object>> verificarVinculo(
            @PathVariable Integer puestoTitularId,
            Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        Integer idPuestoAsistente = vinculoService.resolverPuestoVigente(idUsuario);
        boolean vigente = idPuestoAsistente != null
                && vinculoService.esVigente(idPuestoAsistente, puestoTitularId);
        return ResponseEntity.ok(Map.of(
                "vigente", vigente,
                "puestoAsistente", idPuestoAsistente != null ? idPuestoAsistente : -1,
                "puestoTitular", puestoTitularId
        ));
    }
}
