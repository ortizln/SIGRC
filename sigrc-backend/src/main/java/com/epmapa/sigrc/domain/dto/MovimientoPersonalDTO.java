package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record MovimientoPersonalDTO(
    Integer idMovimiento,
    Integer idEmpleado,
    String nombreEmpleado,
    String tipoMovimiento,
    Integer idAsignacionOrigen,
    String asignacionOrigenDescripcion,
    Integer idPuestoDestino,
    String puestoDestinoNombre,
    Integer idUnidadDestino,
    String unidadDestinoNombre,
    LocalDate fechaSolicitud,
    LocalDate fechaDesde,
    LocalDate fechaHasta,
    String motivo,
    Integer documentoRespaldoId,
    String estado,
    Integer creadoPor,
    Integer aprobadoPor
) {}