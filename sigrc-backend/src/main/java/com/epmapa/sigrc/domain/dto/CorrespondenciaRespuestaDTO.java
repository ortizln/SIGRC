package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CorrespondenciaRespuestaDTO(
    Integer idRespuesta,
    Integer idCorrespondencia,
    LocalDate fechaRespuesta,
    String numeroDocumento,
    Integer idTipoDocumento,
    String tipoDocumentoNombre,
    Integer idResponsable,
    String responsableNombre,
    String observaciones,
    LocalDateTime creadoEn
) {}
