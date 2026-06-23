package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record VersionDTO(
    Integer idVersion,
    String version,
    String tipo,
    String descripcion,
    String notasLiberacion,
    String estado,
    String ambiente,
    LocalDateTime fechaDespliegue,
    LocalDateTime creadoEn,
    Boolean activo,
    Integer idSistema,
    String nombreSistema,
    Integer idCambio,
    String codigoCambio,
    Integer idResponsable,
    String nombreResponsable
) {}
