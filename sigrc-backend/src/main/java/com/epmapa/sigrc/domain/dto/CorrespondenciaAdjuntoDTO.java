package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record CorrespondenciaAdjuntoDTO(
    Integer idAdjunto,
    Integer idCorrespondencia,
    String tipo,
    String nombreOriginal,
    String nombreArchivo,
    String tipoMime,
    Long tamanoBytes,
    String hashSha256,
    Integer idUsuario,
    String usuarioNombre,
    LocalDateTime creadoEn
) {}
