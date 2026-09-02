package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.AsignacionDTO;
import com.epmapa.sigrc.domain.dto.AsignacionRequest;
import com.epmapa.sigrc.domain.dto.DashboardTalentoHumanoDTO;
import com.epmapa.sigrc.domain.dto.DistributivoDTO;
import com.epmapa.sigrc.domain.dto.EmpleadoDTO;
import com.epmapa.sigrc.domain.dto.EmpleadoRequest;
import com.epmapa.sigrc.domain.dto.JefeInfoDTO;
import com.epmapa.sigrc.domain.dto.MatrizPersonaPuestoDTO;
import com.epmapa.sigrc.domain.dto.MigracionTHRequest;
import com.epmapa.sigrc.domain.dto.MigracionTHResultadoDTO;
import com.epmapa.sigrc.domain.dto.NivelOrganizacionalDTO;
import com.epmapa.sigrc.domain.dto.NodoOrganigramaDTO;
import com.epmapa.sigrc.domain.dto.PuestoDTO;
import com.epmapa.sigrc.domain.dto.PuestoRequest;
import com.epmapa.sigrc.domain.dto.UnidadOrganizacionalDTO;
import com.epmapa.sigrc.domain.dto.UnidadOrganizacionalRequest;
import com.epmapa.sigrc.domain.entity.Empleado;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.service.AsignacionPuestoService;
import com.epmapa.sigrc.domain.service.DistributivoExportService;
import com.epmapa.sigrc.domain.service.EmpleadoDocumentoFileService;
import com.epmapa.sigrc.domain.service.EmpleadoService;
import com.epmapa.sigrc.domain.service.EstructuraOrganizacionalService;
import com.epmapa.sigrc.domain.service.MigracionTHService;
import com.epmapa.sigrc.domain.service.PuestoService;
import com.epmapa.sigrc.domain.service.ReportesTalentoHumanoService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/talento-humano")
@Tag(name = "Talento Humano", description = "Estructura organizacional y módulo de Talento Humano")
public class TalentoHumanoController {

    private final EstructuraOrganizacionalService estructuraService;
    private final PuestoService puestoService;
    private final EmpleadoService empleadoService;
    private final AsignacionPuestoService asignacionService;
    private final ReportesTalentoHumanoService reporteService;
    private final DistributivoExportService exportService;
    private final EmpleadoDocumentoFileService documentoFileService;
    private final MigracionTHService migracionService;

    public TalentoHumanoController(EstructuraOrganizacionalService estructuraService,
                                   PuestoService puestoService,
                                   EmpleadoService empleadoService,
                                   AsignacionPuestoService asignacionService,
                                   ReportesTalentoHumanoService reporteService,
                                   DistributivoExportService exportService,
                                   EmpleadoDocumentoFileService documentoFileService,
                                   MigracionTHService migracionService) {
        this.estructuraService = estructuraService;
        this.puestoService = puestoService;
        this.empleadoService = empleadoService;
        this.asignacionService = asignacionService;
        this.reporteService = reporteService;
        this.exportService = exportService;
        this.documentoFileService = documentoFileService;
        this.migracionService = migracionService;
    }

    // ---------- Niveles organizacionales ----------

    @GetMapping("/niveles-organizacionales")
    @Operation(summary = "Listar niveles organizacionales")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NivelOrganizacionalDTO>> niveles() {
        return ResponseEntity.ok(estructuraService.listarNiveles());
    }

    @PostMapping("/niveles-organizacionales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nivel organizacional")
    @Transactional
    public ResponseEntity<NivelOrganizacionalDTO> crearNivel(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(estructuraService.crearNivel(
            (String) body.get("codigo"),
            (String) body.get("nombre"),
            (String) body.get("descripcion"),
            body.get("orden") != null ? ((Number) body.get("orden")).intValue() : null));
    }

    @PutMapping("/niveles-organizacionales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar nivel organizacional")
    @Transactional
    public ResponseEntity<NivelOrganizacionalDTO> actualizarNivel(@PathVariable Integer id,
                                                                  @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(estructuraService.actualizarNivel(
            id,
            (String) body.get("codigo"),
            (String) body.get("nombre"),
            (String) body.get("descripcion"),
            body.get("orden") != null ? ((Number) body.get("orden")).intValue() : null));
    }

    @DeleteMapping("/niveles-organizacionales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar nivel organizacional")
    @Transactional
    public ResponseEntity<Void> desactivarNivel(@PathVariable Integer id) {
        estructuraService.desactivarNivel(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Unidades organizacionales ----------

    @GetMapping("/unidades")
    @Operation(summary = "Listar unidades organizacionales")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UnidadOrganizacionalDTO>> unidades() {
        return ResponseEntity.ok(estructuraService.listarUnidades());
    }

    @GetMapping("/unidades/{id}")
    @Operation(summary = "Obtener unidad organizacional")
    @Transactional(readOnly = true)
    public ResponseEntity<UnidadOrganizacionalDTO> unidad(@PathVariable Integer id) {
        return ResponseEntity.ok(estructuraService.obtenerUnidad(id));
    }

    @PostMapping("/unidades")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear unidad organizacional")
    @Transactional
    public ResponseEntity<UnidadOrganizacionalDTO> crearUnidad(@RequestBody UnidadOrganizacionalRequest req) {
        return ResponseEntity.ok(estructuraService.crearUnidad(req));
    }

    @PutMapping("/unidades/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar unidad organizacional")
    @Transactional
    public ResponseEntity<UnidadOrganizacionalDTO> actualizarUnidad(@PathVariable Integer id,
                                                                    @RequestBody UnidadOrganizacionalRequest req) {
        return ResponseEntity.ok(estructuraService.actualizarUnidad(id, req));
    }

    @DeleteMapping("/unidades/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar unidad organizacional")
    @Transactional
    public ResponseEntity<Void> desactivarUnidad(@PathVariable Integer id) {
        estructuraService.desactivarUnidad(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/unidades/{id}/mover")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mover unidad a otro padre (drag & drop)")
    @Transactional
    public ResponseEntity<UnidadOrganizacionalDTO> moverUnidad(@PathVariable Integer id,
                                                                @RequestBody Map<String, Object> body) {
        Integer idNuevoPadre = body.get("idUnidadPadre") != null
            ? ((Number) body.get("idUnidadPadre")).intValue() : null;
        return ResponseEntity.ok(estructuraService.moverUnidad(id, idNuevoPadre));
    }

    @PutMapping("/unidades/{id}/responsable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignar responsable de una unidad (por asignación de puesto)")
    @Transactional
    public ResponseEntity<UnidadOrganizacionalDTO> asignarResponsableUnidad(@PathVariable Integer id,
                                                                            @RequestBody Map<String, Object> body) {
        Integer responsableAsignacionId = body.get("responsableAsignacionId") != null
            ? ((Number) body.get("responsableAsignacionId")).intValue() : null;
        return ResponseEntity.ok(estructuraService.asignarResponsable(id, responsableAsignacionId));
    }

    // ---------- Organigrama ----------

    @GetMapping("/organigrama")
    @Operation(summary = "Obtener organigrama (árbol jerárquico)")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NodoOrganigramaDTO>> organigrama() {
        return ResponseEntity.ok(estructuraService.organigrama());
    }

    // ---------- Puestos ----------

    @GetMapping("/puestos")
    @Operation(summary = "Listar puestos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PuestoDTO>> puestos() {
        return ResponseEntity.ok(puestoService.listar());
    }

    @GetMapping("/puestos/{id}/perfil")
    @Operation(summary = "Obtener perfil de un puesto")
    @Transactional(readOnly = true)
    public ResponseEntity<Puesto> perfilPuesto(@PathVariable Integer id) {
        return ResponseEntity.ok(puestoService.obtenerConPerfil(id));
    }

    @PostMapping("/puestos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear puesto con perfil")
    @Transactional
    public ResponseEntity<PuestoDTO> crearPuesto(@RequestBody PuestoRequest req) {
        return ResponseEntity.ok(puestoService.crear(req));
    }

    @PutMapping("/puestos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar puesto y su perfil")
    @Transactional
    public ResponseEntity<PuestoDTO> actualizarPuesto(@PathVariable Integer id,
                                                      @RequestBody PuestoRequest req) {
        return ResponseEntity.ok(puestoService.actualizar(id, req));
    }

    @DeleteMapping("/puestos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar puesto")
    @Transactional
    public ResponseEntity<Void> desactivarPuesto(@PathVariable Integer id) {
        puestoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Empleados ----------

    @GetMapping("/empleados")
    @Operation(summary = "Listar empleados")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EmpleadoDTO>> empleados() {
        return ResponseEntity.ok(empleadoService.listar());
    }

    @GetMapping("/empleados/{id}/expediente")
    @Operation(summary = "Obtener expediente completo de un empleado (control de acceso y confidencialidad)")
    @Transactional(readOnly = true)
    public ResponseEntity<Empleado> expedienteEmpleado(@PathVariable Integer id,
                                                       Authentication auth,
                                                       HttpServletRequest request) {
        var principal = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(empleadoService.obtenerConExpedienteAutorizado(
            id, principal.idUsuario(), auth.getName(), request));
    }

    @GetMapping("/mi-expediente")
    @Operation(summary = "Expediente del propio empleado (auto-consulta con confidencialidad §30)")
    @Transactional(readOnly = true)
    public ResponseEntity<Empleado> miExpediente(Authentication auth, HttpServletRequest request) {
        var principal = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(empleadoService.obtenerMiExpediente(
            principal.idUsuario(), auth.getName(), request));
    }

    @PostMapping("/empleados/{id}/documentos/{idDocumento}/archivo")
    @Operation(summary = "Subir archivo a un documento del expediente (ADMIN o permiso TH)")
    @Transactional
    public ResponseEntity<?> subirArchivoExpediente(@PathVariable Integer id,
                                                    @PathVariable Integer idDocumento,
                                                    @RequestParam("file") MultipartFile file,
                                                    Authentication auth) throws IOException {
        var principal = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(documentoFileService.subirArchivo(id, idDocumento, file, principal.idUsuario()));
    }

    @GetMapping("/empleados/{id}/documentos/{idDocumento}/descargar")
    @Operation(summary = "Descargar archivo de un documento del expediente (control de confidencialidad §30)")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargarArchivoExpediente(@PathVariable Integer id,
                                                               @PathVariable Integer idDocumento,
                                                               Authentication auth,
                                                               HttpServletRequest request) throws IOException {
        var principal = (UserPrincipal) auth.getPrincipal();
        var documento = documentoFileService.descargar(id, idDocumento, principal.idUsuario(), request);
        Resource resource = new UrlResource(Paths.get(documento.getRutaArchivo()).toUri());
        String nombre = documento.getNombreArchivo() != null ? documento.getNombreArchivo()
            : Paths.get(documento.getRutaArchivo()).getFileName().toString();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
            .body(resource);
    }

    @PostMapping("/empleados")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear empleado con expediente")
    @Transactional
    public ResponseEntity<EmpleadoDTO> crearEmpleado(@RequestBody EmpleadoRequest req) {
        return ResponseEntity.ok(empleadoService.crear(req));
    }

    @PutMapping("/empleados/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar empleado y su expediente")
    @Transactional
    public ResponseEntity<EmpleadoDTO> actualizarEmpleado(@PathVariable Integer id,
                                                          @RequestBody EmpleadoRequest req) {
        return ResponseEntity.ok(empleadoService.actualizar(id, req));
    }

    @DeleteMapping("/empleados/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar empleado")
    @Transactional
    public ResponseEntity<Void> desactivarEmpleado(@PathVariable Integer id) {
        empleadoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Asignaciones ----------

    @GetMapping("/asignaciones")
    @Operation(summary = "Historial de asignaciones de un empleado")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AsignacionDTO>> asignaciones(@RequestParam Integer empleadoId) {
        return ResponseEntity.ok(asignacionService.listarPorEmpleado(empleadoId));
    }

    @GetMapping("/asignaciones/actual")
    @Operation(summary = "Asignación vigente de un empleado")
    @Transactional(readOnly = true)
    public ResponseEntity<AsignacionDTO> asignacionActual(@RequestParam Integer empleadoId) {
        return ResponseEntity.ok(asignacionService.obtenerActual(empleadoId));
    }

    @GetMapping("/asignaciones/todas")
    @Operation(summary = "Todas las asignaciones activas (para seleccionar encargado)")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AsignacionDTO>> todasAsignacionesActivas() {
        return ResponseEntity.ok(asignacionService.listarTodasActivas());
    }

    @PostMapping("/asignaciones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignar puesto a un empleado (cierra la asignación anterior)")
    @Transactional
    public ResponseEntity<AsignacionDTO> asignar(@RequestBody AsignacionRequest req) {
        return ResponseEntity.ok(asignacionService.asignar(req));
    }

    @PostMapping("/asignaciones/{id}/finalizar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Finalizar asignación de puesto")
    @Transactional
    public ResponseEntity<AsignacionDTO> finalizarAsignacion(@PathVariable Integer id) {
        return ResponseEntity.ok(asignacionService.finalizar(id));
    }

    @GetMapping("/jefatura/{idEmpleado}")
    @Operation(summary = "Obtener jefe inmediato por estructura organizacional")
    @Transactional(readOnly = true)
    public ResponseEntity<JefeInfoDTO> jefeInmediato(@PathVariable Integer idEmpleado) {
        return ResponseEntity.ok(asignacionService.jefeInmediato(idEmpleado));
    }

    // ---------- Reportes y control ----------

    @GetMapping("/distributivo")
    @Operation(summary = "Distributivo de personal con filtros")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DistributivoDTO>> distributivo(
            @RequestParam(required = false) Integer idUnidad,
            @RequestParam(required = false) Integer idPuesto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoPersonal) {
        return ResponseEntity.ok(reporteService.distributivo(idUnidad, idPuesto, estado, tipoPersonal));
    }

    @GetMapping("/distributivo/exportar")
    @Operation(summary = "Exportar distributivo de personal a Excel (.xlsx) o PDF")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportarDistributivo(
            @RequestParam(required = false) Integer idUnidad,
            @RequestParam(required = false) Integer idPuesto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoPersonal,
            @RequestParam(defaultValue = "excel") String formato) throws IOException {
        var datos = reporteService.distributivo(idUnidad, idPuesto, estado, tipoPersonal);
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] bytes = pdf ? exportService.pdfDistributivo(datos) : exportService.excelDistributivo(datos);
        String nombre = "distributivo_personal_" + LocalDate.now() + (pdf ? ".pdf" : ".xlsx");
        MediaType tipo = pdf
            ? MediaType.APPLICATION_PDF
            : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok()
            .contentType(tipo)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
            .body(bytes);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Indicadores del dashboard de Talento Humano")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardTalentoHumanoDTO> dashboard() {
        return ResponseEntity.ok(reporteService.dashboard());
    }

    @GetMapping("/matriz-persona-puesto/{idEmpleado}")
    @Operation(summary = "Comparar requisitos del puesto contra el expediente del empleado")
    @Transactional(readOnly = true)
    public ResponseEntity<MatrizPersonaPuestoDTO> matriz(@PathVariable Integer idEmpleado) {
        return ResponseEntity.ok(reporteService.matrizPersonaPuesto(idEmpleado));
    }

    @PostMapping("/migracion/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Migración §35: usuarios → empleados + asignaciones (Fases 1-5, idempotente)",
        description = "dryRun=true solo reporta sin persistir. Nunca borra datos ni retira los campos "
            + "antiguos usuario.area / usuario.cargo.")
    public ResponseEntity<MigracionTHResultadoDTO> migrarUsuarios(@RequestBody(required = false) MigracionTHRequest body) {
        boolean dryRun = body == null || body.dryRun();
        return ResponseEntity.ok(migracionService.migrarUsuarios(dryRun));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
