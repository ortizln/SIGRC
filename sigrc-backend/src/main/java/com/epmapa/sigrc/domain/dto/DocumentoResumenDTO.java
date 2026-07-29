package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record DocumentoResumenDTO(
    Integer id,
    String numeroInterno,
    String codigoDocumento,
    String asunto,
    String estado,
    String prioridad,
    String departamentoRemitente,
    String fechaLimiteRespuesta,
    String creadoEn
) {}
