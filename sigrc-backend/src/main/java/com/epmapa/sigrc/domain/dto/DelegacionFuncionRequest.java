package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record DelegacionFuncionRequest(
    Integer idAsignacionOrigen,
    Integer idAsignacionDelegada,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String tipo,
    String alcance,
    Long documentoRespaldoId,
    String observacion
) {}