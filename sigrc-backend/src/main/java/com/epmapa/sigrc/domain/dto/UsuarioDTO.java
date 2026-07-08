package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record UsuarioDTO(
    Integer idUsuario,
    String username,
    String email,
    String nombres,
    String apellidos,
    String nombreCompleto,
    String cargo,
    String areaNombre,
    Integer idArea,
    String rolCodigo,
    String rolNombre,
    String telefono,
    Boolean activo,
    Boolean debeCambiarPassword,
    Boolean bloqueado,
    List<UsuarioPermisoDTO> permisos
) {}
