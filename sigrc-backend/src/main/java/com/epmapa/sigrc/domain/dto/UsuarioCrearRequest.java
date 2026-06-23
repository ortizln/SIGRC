package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioCrearRequest(
    @NotBlank String username,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String nombres,
    @NotBlank String apellidos,
    String cargo,
    String telefono,
    Integer idArea,
    @NotBlank String rolCodigo
) {}
