package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record DistributivoDTO(
    Integer idEmpleado,
    String identificacion,
    String funcionario,
    Integer idUnidad,
    String unidad,
    Integer idPuesto,
    String puesto,
    String grupoOcupacional,
    String tipoRelacion,
    LocalDate fechaIngreso,
    String estadoLaboral,
    String tipoPersonal
) {}