package com.epmapa.sigrc.web.controller;

import com.epmapa.sigrc.domain.dto.LoginRequest;
import com.epmapa.sigrc.domain.dto.LoginResponse;
import com.epmapa.sigrc.domain.dto.UsuarioDTO;
import com.epmapa.sigrc.domain.service.AuthService;
import com.epmapa.sigrc.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Endpoints de autenticación y login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener datos del usuario autenticado",
        description = "Devuelve el usuario, su empleado vinculado, asignación vigente, puesto, unidad, rol y permisos")
    public ResponseEntity<UsuarioDTO> me(Authentication auth) {
        Integer idUsuario = ((UserPrincipal) auth.getPrincipal()).idUsuario();
        return ResponseEntity.ok(authService.obtenerMe(idUsuario));
    }
}
