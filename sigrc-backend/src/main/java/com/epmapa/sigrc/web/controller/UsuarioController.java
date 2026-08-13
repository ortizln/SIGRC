package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.PaginacionDTO;
import com.epmapa.sigrc.domain.dto.UsuarioActualizarRequest;
import com.epmapa.sigrc.domain.dto.UsuarioCrearRequest;
import com.epmapa.sigrc.domain.dto.UsuarioDTO;
import com.epmapa.sigrc.domain.dto.UsuarioPermisoDTO;
import com.epmapa.sigrc.domain.service.UsuarioService;
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

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios activos")
    public ResponseEntity<List<UsuarioDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarActivos());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios con paginación y búsqueda")
    public ResponseEntity<PaginacionDTO<UsuarioDTO>> listarPaginado(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(required = false) String texto) {
        return ResponseEntity.ok(usuarioService.listarPaginado(pagina, tamanio, texto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<UsuarioDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario")
    public ResponseEntity<UsuarioDTO> crear(@Valid @RequestBody UsuarioCrearRequest request) {
        return ResponseEntity.ok(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<UsuarioDTO> actualizar(@PathVariable Integer id,
                                                   @Valid @RequestBody UsuarioActualizarRequest request,
                                                   Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(usuarioService.actualizar(id, request, idUsuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar usuario")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener permisos de módulos del usuario")
    public ResponseEntity<List<UsuarioPermisoDTO>> obtenerPermisos(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPermisos(id));
    }

    @PutMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Guardar permisos de módulos del usuario")
    public ResponseEntity<List<UsuarioPermisoDTO>> guardarPermisos(@PathVariable Integer id,
                                                                     @RequestBody List<UsuarioPermisoDTO> permisos) {
        return ResponseEntity.ok(usuarioService.guardarPermisos(id, permisos));
    }

    @GetMapping("/sin-empleado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios activos sin empleado vinculado")
    public ResponseEntity<List<UsuarioDTO>> listarSinEmpleado() {
        return ResponseEntity.ok(usuarioService.listarActivosSinEmpleado());
    }

    @PutMapping("/{id}/empleado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vincular (o desvincular) un empleado a un usuario",
        description = "Enviar idEmpleado = null para desvincular")
    public ResponseEntity<UsuarioDTO> vincularEmpleado(@PathVariable Integer id,
                                                        @RequestBody(required = false) Map<String, Object> body) {
        Integer idEmpleado = body != null && body.get("idEmpleado") != null
            ? ((Number) body.get("idEmpleado")).intValue() : null;
        return ResponseEntity.ok(usuarioService.vincularEmpleado(id, idEmpleado));
    }
}
