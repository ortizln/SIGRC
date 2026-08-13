package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record MovimientoPersonalRequest(
    Integer idEmpleado,
    String tipoMovimiento,
    Integer idAsignacionOrigen,
    Integer idPuestoDestino,
    Integer idUnidadDestino,
    LocalDate fechaSolicitud,
    LocalDate fechaDesde,
    LocalDate fechaHasta,
    String motivo,
    Integer documentoRespaldoId
) {}