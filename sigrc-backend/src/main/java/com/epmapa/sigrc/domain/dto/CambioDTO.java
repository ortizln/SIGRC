package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;

public record CambioDTO(
    Integer idCambio,
    String codigoCambio,
    String titulo,
    String descripcion,
    String justificacion,
    String tipo,
    String impacto,
    String riesgo,
    String estado,
    String planImplementacion,
    String planRetorno,
    String planArchivo,
    LocalDateTime fechaAprobacion,
    LocalDateTime fechaInicio,
    LocalDateTime fechaImplementacion,
    LocalDateTime fechaVerificacion,
    String resultado,
    String leccionesAprendidas,
    LocalDateTime creadoEn,
    Integer idTicket,
    String ticketNumero,
    Integer idSistema,
    String sistemaNombre,
    Integer idSolicitante,
    String solicitanteNombre,
    Integer idAprobador,
    String aprobadorNombre,
    Integer idResponsable,
    String responsableNombre
) {}
