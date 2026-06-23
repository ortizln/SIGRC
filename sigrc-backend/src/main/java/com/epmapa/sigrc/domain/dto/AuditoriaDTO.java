package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record AuditoriaDTO(
    Integer idAuditoria,
    String username,
    String accion,
    String tipoOperacion,
    String tablaAfectada,
    Integer idRegistro,
    String datosAnteriores,
    String datosNuevos,
    String direccionIp,
    String userAgent,
    String resultado,
    String detalle,
    LocalDateTime creadoEn
) {}
