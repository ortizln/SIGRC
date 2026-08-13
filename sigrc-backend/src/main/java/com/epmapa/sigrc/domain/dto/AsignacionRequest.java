package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record AsignacionRequest(
    Integer idEmpleado,
    Integer idPuesto,
    Integer idUnidad,
    String tipoAsignacion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String observacion
) {}
