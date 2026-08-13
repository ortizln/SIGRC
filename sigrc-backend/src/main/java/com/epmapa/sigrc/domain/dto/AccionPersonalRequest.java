package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record AccionPersonalRequest(
    Integer idEmpleado,
    String tipo,
    LocalDate fechaEmision,
    LocalDate fechaVigenciaDesde,
    LocalDate fechaVigenciaHasta,
    String motivo,
    String situacionActual,
    String situacionPropuesta,
    Integer documentoId
) {}