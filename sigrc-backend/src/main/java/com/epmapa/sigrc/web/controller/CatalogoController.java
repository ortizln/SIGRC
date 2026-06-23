package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.entity.*;
import com.epmapa.sigrc.domain.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<Area>> areas() {
        return ResponseEntity.ok(areaRepository.findByActivoTrueOrderByNombre());
    }

    @PostMapping("/areas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear área")
    public ResponseEntity<Area> crearArea(@RequestBody Area area) {
        area.setIdArea(null);
        area.setActivo(true);
        return ResponseEntity.ok(areaRepository.save(area));
    }

    @PutMapping("/areas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar área")
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
    public ResponseEntity<Void> eliminarArea(@PathVariable Integer id) {
        var area = areaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Área no encontrada: " + id));
        area.setActivo(false);
        areaRepository.save(area);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sistemas")
    @Operation(summary = "Listar sistemas")
    public ResponseEntity<List<Sistema>> sistemas() {
        return ResponseEntity.ok(sistemaRepository.findByActivoTrueOrderByNombre());
    }

    @PostMapping("/sistemas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear sistema")
    public ResponseEntity<Sistema> crearSistema(@RequestBody Sistema sistema) {
        sistema.setIdSistema(null);
        sistema.setActivo(true);
        return ResponseEntity.ok(sistemaRepository.save(sistema));
    }

    @PutMapping("/sistemas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sistema")
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
    public ResponseEntity<Void> eliminarSistema(@PathVariable Integer id) {
        var sistema = sistemaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sistema no encontrado: " + id));
        sistema.setActivo(false);
        sistemaRepository.save(sistema);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías")
    public ResponseEntity<List<Categoria>> categorias() {
        return ResponseEntity.ok(categoriaRepository.findByActivoTrueOrderByNombre());
    }

    @PostMapping("/categorias")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear categoría")
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        categoria.setIdCategoria(null);
        categoria.setActivo(true);
        return ResponseEntity.ok(categoriaRepository.save(categoria));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar categoría")
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
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        var cat = categoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
        cat.setActivo(false);
        categoriaRepository.save(cat);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subcategorias/{idCategoria}")
    @Operation(summary = "Listar subcategorías por categoría")
    public ResponseEntity<List<Subcategoria>> subcategorias(@PathVariable Integer idCategoria) {
        return ResponseEntity.ok(subcategoriaRepository.findByCategoriaIdCategoriaAndActivoTrue(idCategoria));
    }

    @PostMapping("/subcategorias")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear subcategoría")
    public ResponseEntity<Subcategoria> crearSubcategoria(@RequestBody Subcategoria sc) {
        sc.setIdSubcategoria(null);
        sc.setActivo(true);
        return ResponseEntity.ok(subcategoriaRepository.save(sc));
    }

    @PutMapping("/subcategorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar subcategoría")
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
    public ResponseEntity<Void> eliminarSubcategoria(@PathVariable Integer id) {
        var sc = subcategoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Subcategoría no encontrada: " + id));
        sc.setActivo(false);
        subcategoriaRepository.save(sc);
        return ResponseEntity.noContent().build();
    }
}
