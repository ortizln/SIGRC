package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record CorrespondenciaHistorialDTO(
    Integer idHistorial,
    Integer idCorrespondencia,
    String estadoAnterior,
    String estadoNuevo,
    String accion,
    Integer idUsuario,
    String usuarioNombre,
    String detalle,
    LocalDateTime creadoEn
) {}
