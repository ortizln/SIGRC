package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record AccionPersonalDTO(
    Integer idAccion,
    String numero,
    Integer idEmpleado,
    String nombreEmpleado,
    String tipo,
    LocalDate fechaEmision,
    LocalDate fechaVigenciaDesde,
    LocalDate fechaVigenciaHasta,
    String motivo,
    String situacionActual,
    String situacionPropuesta,
    Integer documentoId,
    String estado,
    Integer elaboradoPor,
    Integer revisadoPor,
    Integer aprobadoPor
) {}