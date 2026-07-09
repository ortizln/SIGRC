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
    List<Integer> idsResponsables,
    String prioridad,
    String sentido,
    Boolean requiereRespuesta,
    LocalDate fechaLimiteRespuesta,
    Boolean generaTicket,
    String observaciones,
    List<Integer> areasEtiquetadas,
    List<Integer> idsReferencias,
    List<CorrespondenciaDestinatarioDTO> destinatarios
) {}
