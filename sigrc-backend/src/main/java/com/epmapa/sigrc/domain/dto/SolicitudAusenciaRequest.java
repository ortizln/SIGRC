package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record SolicitudAusenciaRequest(
    Integer idEmpleado,
    String tipo,
    LocalDate fechaDesde,
    LocalDate fechaHasta,
    Integer dias,
    Integer horas,
    String motivo,
    Integer documentoRespaldoId
) {}