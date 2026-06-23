package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record LoginResponse(
    String token,
    String refreshToken,
    String tipo,
    LocalDateTime expiracion,
    UsuarioDTO usuario
) {}
