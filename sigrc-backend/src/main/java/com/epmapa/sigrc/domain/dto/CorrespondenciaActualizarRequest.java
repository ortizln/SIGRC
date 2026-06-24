package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;
import java.util.List;

public record CorrespondenciaActualizarRequest(
    String asunto,
    String resumenEjecutivo,
    String codigoDocumento,
    Integer idTipoDocumento,
    LocalDate fechaDocumento,
    LocalDate fechaRecepcion,
    String horaRecepcion,
    String personaEntrega,
    String cargo,
    String institucion,
    String departamentoRemitente,
    Integer idResponsable,
    String prioridad,
    Boolean requiereRespuesta,
    LocalDate fechaLimiteRespuesta,
    Boolean generaTicket,
    String observaciones,
    List<Integer> areasEtiquetadas
) {}
