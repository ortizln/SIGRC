package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.AppMovilDTO;
import com.epmapa.sigrc.domain.service.AppMovilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app-movil")
@Tag(name = "App Móvil", description = "Gestión de versiones del APK de la app móvil")
public class AppMovilController {

    private final AppMovilService appMovilService;

    public AppMovilController(AppMovilService appMovilService) {
        this.appMovilService = appMovilService;
    }

    @GetMapping
    @Operation(summary = "Listar versiones APK")
    public ResponseEntity<List<AppMovilDTO>> listar() {
        return ResponseEntity.ok(appMovilService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener versión APK por ID")
    public ResponseEntity<AppMovilDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(appMovilService.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir nueva versión APK")
    public ResponseEntity<AppMovilDTO> subir(
            @RequestParam String version,
            @RequestParam(required = false) String descripcion,
            @RequestParam MultipartFile archivo) {
        return ResponseEntity.ok(appMovilService.subir(version, descripcion, archivo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar versión APK")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        appMovilService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/descargar/{id}")
    @Operation(summary = "Descargar archivo APK")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer id) throws IOException {
        var dto = appMovilService.obtenerPorId(id);
        Path ruta = Path.of(dto.rutaArchivo());
        byte[] contenido = Files.readAllBytes(ruta);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + dto.nombreArchivo() + "\"")
                .body(contenido);
    }

    @GetMapping("/ultimo")
    @Operation(summary = "Obtener última versión APK activa")
    public ResponseEntity<?> ultimo() {
        var apk = appMovilService.obtenerUltimo();
        if (apk == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(new AppMovilDTO(
                apk.getIdAppMovil(), apk.getVersion(), apk.getNombreArchivo(),
                apk.getRutaArchivo(), apk.getTamanioBytes(), apk.getDescripcion(),
                apk.getChecksum(), apk.getActivo(), apk.getCreadoEn()
        ));
    }

    @GetMapping("/ultimo/descargar")
    @Operation(summary = "Descargar última versión APK")
    public ResponseEntity<?> descargarUltimo() throws IOException {
        var apk = appMovilService.obtenerUltimo();
        if (apk == null) return ResponseEntity.noContent().build();
        Path ruta = Path.of(apk.getRutaArchivo());
        byte[] contenido = Files.readAllBytes(ruta);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + apk.getNombreArchivo() + "\"")
                .body(contenido);
    }
}
