package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalogos")
@Tag(name = "Catálogos", description = "Catálogos del sistema (áreas, sistemas, categorías, etc.)")
public class CatalogoController {

    private final AreaRepository areaRepository;
    private final SistemaRepository sistemaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

    public CatalogoController(AreaRepository areaRepository, SistemaRepository sistemaRepository,
                               CategoriaRepository categoriaRepository,
                               SubcategoriaRepository subcategoriaRepository) {
        this.areaRepository = areaRepository;
        this.sistemaRepository = sistemaRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
    }

    @GetMapping("/areas")
    @Operation(summary = "Listar áreas")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Area>> areas() {
        return ResponseEntity.ok(areaRepository.findByActivoTrueOrderByNombre());
    }

    @PostMapping("/areas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear área")
    @Transactional
    public ResponseEntity<Area> crearArea(@RequestBody Area area) {
        area.setIdArea(null);
        area.setActivo(true);
        return ResponseEntity.ok(areaRepository.save(area));
    }

    @PutMapping("/areas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar área")
    @Transactional
    public ResponseEntity<Area> actualizarArea(@PathVariable Integer id, @RequestBody Area dto) {
        var area = areaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Área no encontrada: " + id));
        if (dto.getCodigo() != null) area.setCodigo(dto.getCodigo());
        if (dto.getNombre() != null) area.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) area.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) area.setActivo(dto.getActivo());
        return ResponseEntity.ok(areaRepository.save(area));
    }

    @DeleteMapping("/areas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar área")
    @Transactional
    public ResponseEntity<Void> eliminarArea(@PathVariable Integer id) {
        var area = areaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Área no encontrada: " + id));
        area.setActivo(false);
        areaRepository.save(area);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/areas/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar área permanentemente")
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminarAreaHard(@PathVariable Integer id) {
        var area = areaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Área no encontrada: " + id));
        try {
            areaRepository.delete(area);
            areaRepository.flush();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No se puede eliminar porque está siendo usado por otros datos."));
        }
    }

    @GetMapping("/sistemas")
    @Operation(summary = "Listar sistemas")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Sistema>> sistemas() {
        return ResponseEntity.ok(sistemaRepository.findByActivoTrueOrderByNombre());
    }

    @PostMapping("/sistemas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear sistema")
    @Transactional
    public ResponseEntity<Sistema> crearSistema(@RequestBody Sistema sistema) {
        sistema.setIdSistema(null);
        sistema.setActivo(true);
        if (sistema.getEstado() == null) sistema.setEstado("ACTIVO");
        return ResponseEntity.ok(sistemaRepository.save(sistema));
    }

    @PutMapping("/sistemas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sistema")
    @Transactional
    public ResponseEntity<Sistema> actualizarSistema(@PathVariable Integer id, @RequestBody Sistema dto) {
        var sistema = sistemaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sistema no encontrado: " + id));
        if (dto.getCodigo() != null) sistema.setCodigo(dto.getCodigo());
        if (dto.getNombre() != null) sistema.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) sistema.setDescripcion(dto.getDescripcion());
        if (dto.getVersionActual() != null) sistema.setVersionActual(dto.getVersionActual());
        if (dto.getTecnologia() != null) sistema.setTecnologia(dto.getTecnologia());
        if (dto.getEstado() != null) sistema.setEstado(dto.getEstado());
        if (dto.getActivo() != null) sistema.setActivo(dto.getActivo());
        return ResponseEntity.ok(sistemaRepository.save(sistema));
    }

    @DeleteMapping("/sistemas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar sistema")
    @Transactional
    public ResponseEntity<Void> eliminarSistema(@PathVariable Integer id) {
        var sistema = sistemaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sistema no encontrado: " + id));
        sistema.setActivo(false);
        sistemaRepository.save(sistema);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sistemas/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar sistema permanentemente")
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminarSistemaHard(@PathVariable Integer id) {
        var sistema = sistemaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sistema no encontrado: " + id));
        try {
            sistemaRepository.delete(sistema);
            sistemaRepository.flush();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No se puede eliminar porque está siendo usado por otros datos."));
        }
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Categoria>> categorias() {
        return ResponseEntity.ok(categoriaRepository.findByActivoTrueOrderByNombre());
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear categoría")
    @Transactional
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        categoria.setIdCategoria(null);
        categoria.setActivo(true);
        return ResponseEntity.ok(categoriaRepository.save(categoria));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar categoría")
    @Transactional
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Integer id, @RequestBody Categoria dto) {
        var cat = categoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
        if (dto.getCodigo() != null) cat.setCodigo(dto.getCodigo());
        if (dto.getNombre() != null) cat.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) cat.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) cat.setActivo(dto.getActivo());
        return ResponseEntity.ok(categoriaRepository.save(cat));
    }

    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar categoría")
    @Transactional
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        var cat = categoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
        cat.setActivo(false);
        categoriaRepository.save(cat);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/categorias/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar categoría permanentemente")
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminarCategoriaHard(@PathVariable Integer id) {
        var cat = categoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
        try {
            categoriaRepository.delete(cat);
            categoriaRepository.flush();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No se puede eliminar porque está siendo usado por otros datos."));
        }
    }

    @GetMapping("/subcategorias/{idCategoria}")
    @Operation(summary = "Listar subcategorías por categoría")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Subcategoria>> subcategorias(@PathVariable Integer idCategoria) {
        return ResponseEntity.ok(subcategoriaRepository.findByCategoriaIdCategoriaAndActivoTrue(idCategoria));
    }

    @PostMapping("/subcategorias")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear subcategoría")
    @Transactional
    public ResponseEntity<Subcategoria> crearSubcategoria(@RequestBody Subcategoria sc) {
        sc.setIdSubcategoria(null);
        sc.setActivo(true);
        return ResponseEntity.ok(subcategoriaRepository.save(sc));
    }

    @PutMapping("/subcategorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar subcategoría")
    @Transactional
    public ResponseEntity<Subcategoria> actualizarSubcategoria(@PathVariable Integer id, @RequestBody Subcategoria dto) {
        var sc = subcategoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Subcategoría no encontrada: " + id));
        if (dto.getCodigo() != null) sc.setCodigo(dto.getCodigo());
        if (dto.getNombre() != null) sc.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) sc.setDescripcion(dto.getDescripcion());
        if (dto.getCategoria() != null) sc.setCategoria(dto.getCategoria());
        if (dto.getActivo() != null) sc.setActivo(dto.getActivo());
        return ResponseEntity.ok(subcategoriaRepository.save(sc));
    }

    @DeleteMapping("/subcategorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar subcategoría")
    @Transactional
    public ResponseEntity<Void> eliminarSubcategoria(@PathVariable Integer id) {
        var sc = subcategoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Subcategoría no encontrada: " + id));
        sc.setActivo(false);
        subcategoriaRepository.save(sc);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subcategorias/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar subcategoría permanentemente")
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminarSubcategoriaHard(@PathVariable Integer id) {
        var sc = subcategoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Subcategoría no encontrada: " + id));
        try {
            subcategoriaRepository.delete(sc);
            subcategoriaRepository.flush();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No se puede eliminar porque está siendo usado por otros datos."));
        }
    }

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Poblar datos iniciales de catálogos")
    @Transactional
    public ResponseEntity<Map<String, Integer>> seed() {
        int areas = 0, sistemas = 0, categorias = 0, subcategorias = 0;

        if (areaRepository.count() == 0) {
            areaRepository.saveAll(List.of(
                Area.builder().codigo("ADM").nombre("Administración").descripcion("Administración General").build(),
                Area.builder().codigo("SIST").nombre("Sistemas").descripcion("Departamento de Sistemas").build(),
                Area.builder().codigo("CONT").nombre("Contabilidad").descripcion("Departamento de Contabilidad").build(),
                Area.builder().codigo("TAL_HUM").nombre("Talento Humano").descripcion("Departamento de Talento Humano").build(),
                Area.builder().codigo("FIN").nombre("Financiero").descripcion("Departamento Financiero").build(),
                Area.builder().codigo("LEG").nombre("Asesoría Legal").descripcion("Asesoría Legal").build(),
                Area.builder().codigo("OPE").nombre("Operaciones").descripcion("Departamento de Operaciones").build(),
                Area.builder().codigo("COM").nombre("Comercialización").descripcion("Departamento Comercial").build()
            ));
            areas = 8;
        }

        if (sistemaRepository.count() == 0) {
            sistemaRepository.saveAll(List.of(
                Sistema.builder().codigo("SIGRC").nombre("SIGRC - Sistema de Gestión de Requerimientos, Cambios y Auditoría").descripcion("Sistema principal de gestión de TI").versionActual("1.0.0").tecnologia("Java / Angular").build(),
                Sistema.builder().codigo("CONTABLE").nombre("Sistema Contable").descripcion("Sistema de contabilidad general").versionActual("3.2.1").tecnologia("PHP / MySQL").build(),
                Sistema.builder().codigo("RRHH").nombre("Sistema de Recursos Humanos").descripcion("Gestión de personal").versionActual("2.0.0").tecnologia("C# / SQL Server").build(),
                Sistema.builder().codigo("FACTUR").nombre("Sistema de Facturación").descripcion("Emisión de facturas y recibos").versionActual("4.1.0").tecnologia("PHP / PostgreSQL").build(),
                Sistema.builder().codigo("SGA").nombre("Sistema de Gestión de Agua").descripcion("Control de consumo y cortes de agua").versionActual("5.0.0").tecnologia("Java / Oracle").build()
            ));
            sistemas = 5;
        }

        if (categoriaRepository.count() == 0) {
            var categoriasList = new ArrayList<Categoria>();
            categoriasList.add(Categoria.builder().codigo("INC").nombre("Incidencia").descripcion("Reporte de fallas o problemas en sistemas").build());
            categoriasList.add(Categoria.builder().codigo("REQ").nombre("Requerimiento").descripcion("Solicitud de nueva funcionalidad o mejora").build());
            categoriasList.add(Categoria.builder().codigo("CONS").nombre("Consulta").descripcion("Consulta sobre el funcionamiento del sistema").build());
            categoriasList.add(Categoria.builder().codigo("SOP").nombre("Soporte").descripcion("Solicitud de soporte técnico").build());
            categoriasList.add(Categoria.builder().codigo("MTO").nombre("Mantenimiento").descripcion("Mantenimiento preventivo o correctivo").build());
            categoriaRepository.saveAll(categoriasList);
            categorias = 5;

            var incidencia = categoriasList.get(0);
            var requerimiento = categoriasList.get(1);

            subcategoriaRepository.saveAll(List.of(
                Subcategoria.builder().codigo("INC-SIS").nombre("Falla de Sistema").descripcion("El sistema no responde o presenta errores").categoria(incidencia).build(),
                Subcategoria.builder().codigo("INC-RED").nombre("Problema de Red").descripcion("Problemas de conectividad").categoria(incidencia).build(),
                Subcategoria.builder().codigo("INC-HW").nombre("Falla de Hardware").descripcion("Problemas con equipos físicos").categoria(incidencia).build(),
                Subcategoria.builder().codigo("INC-SEG").nombre("Incidente de Seguridad").descripcion("Posible vulnerabilidad o acceso no autorizado").categoria(incidencia).build(),
                Subcategoria.builder().codigo("REQ-NVO").nombre("Nueva Funcionalidad").descripcion("Solicitud de una nueva funcionalidad").categoria(requerimiento).build(),
                Subcategoria.builder().codigo("REQ-MEJ").nombre("Mejora").descripcion("Solicitud de mejora de funcionalidad existente").categoria(requerimiento).build(),
                Subcategoria.builder().codigo("REQ-INT").nombre("Integración").descripcion("Solicitud de integración con otro sistema").categoria(requerimiento).build()
            ));
            subcategorias = 7;
        }

        var result = Map.of(
            "areas", areas,
            "sistemas", sistemas,
            "categorias", categorias,
            "subcategorias", subcategorias
        );
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "El código ingresado ya existe en la base de datos."));
    }
}
