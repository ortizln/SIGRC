package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.Email;
import java.util.List;

public record UsuarioActualizarRequest(
    String username,
    @Email String email,
    String password,
    String nombres,
    String apellidos,
    String cargo,
    String telefono,
    Integer idArea,
    String rolCodigo,
    List<UsuarioPermisoDTO> permisos
) {}
