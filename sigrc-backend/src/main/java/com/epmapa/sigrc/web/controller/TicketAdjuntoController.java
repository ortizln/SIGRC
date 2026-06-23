package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.entity.TicketAdjunto;
import com.epmapa.sigrc.domain.service.TicketAdjuntoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets/{idTicket}/adjuntos")
public class TicketAdjuntoController {

    private final TicketAdjuntoService adjuntoService;

    public TicketAdjuntoController(TicketAdjuntoService adjuntoService) {
        this.adjuntoService = adjuntoService;
    }

    @GetMapping
    public ResponseEntity<List<TicketAdjunto>> listar(@PathVariable Integer idTicket) {
        return ResponseEntity.ok(adjuntoService.listarPorTicket(idTicket));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketAdjunto> subir(@PathVariable Integer idTicket,
                                                @RequestParam("archivo") MultipartFile archivo,
                                                @RequestParam("idUsuario") Integer idUsuario) {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(adjuntoService.subir(idTicket, idUsuario, archivo));
    }

    @GetMapping("/{idAdjunto}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Integer idTicket,
                                               @PathVariable Integer idAdjunto) {
        var adjunto = adjuntoService.obtenerInfo(idAdjunto);
        var resource = adjuntoService.descargar(idAdjunto);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(adjunto.getTipoMime()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + adjunto.getNombreArchivo() + "\"")
            .body(resource);
    }

    @GetMapping("/{idAdjunto}")
    public ResponseEntity<TicketAdjunto> obtenerInfo(@PathVariable Integer idTicket,
                                                      @PathVariable Integer idAdjunto) {
        return ResponseEntity.ok(adjuntoService.obtenerInfo(idAdjunto));
    }

    @DeleteMapping("/{idAdjunto}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idTicket,
                                          @PathVariable Integer idAdjunto) {
        adjuntoService.eliminar(idAdjunto);
        return ResponseEntity.noContent().build();
    }
}
