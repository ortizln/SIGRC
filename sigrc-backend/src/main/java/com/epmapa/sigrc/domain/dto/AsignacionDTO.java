package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record AsignacionDTO(
    Integer idAsignacion,
    Integer idEmpleado,
    String nombreEmpleado,
    Integer idPuesto,
    String puestoCodigo,
    String puestoNombre,
    Integer idUnidad,
    String unidadNombre,
    String tipoAsignacion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Boolean esPrincipal,
    String estado,
    String observacion
) {}
