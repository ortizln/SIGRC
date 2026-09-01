package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record SolicitudAusenciaDTO(
    Integer idSolicitud,
    Integer idEmpleado,
    String nombreEmpleado,
    String tipo,
    LocalDate fechaDesde,
    LocalDate fechaHasta,
    Integer dias,
    Integer horas,
    String motivo,
    Integer documentoRespaldoId,
    Integer encargadoAsignacionId,
    String encargadoNombre,
    String estado,
    Integer jefeAprobador,
    Integer thAprobador
) {}