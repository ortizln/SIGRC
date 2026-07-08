package com.epmapa.sigrc.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CorrespondenciaCrearRequest(
    @NotBlank String asunto,
    String resumenEjecutivo,
    @NotBlank String codigoDocumento,
    @NotNull Integer idTipoDocumento,
    @NotNull LocalDate fechaDocumento,
    @NotNull LocalDate fechaRecepcion,
    @NotBlank String horaRecepcion,
    @NotBlank String personaEntrega,
    String cargo,
    String institucion,
    String departamentoRemitente,
    Integer idResponsable,
    @NotBlank String prioridad,
    String sentido,
    Boolean requiereRespuesta,
    LocalDate fechaLimiteRespuesta,
    Boolean generaTicket,
    String observaciones,
    List<Integer> areasEtiquetadas,
    List<Integer> idsReferencias
) {}
