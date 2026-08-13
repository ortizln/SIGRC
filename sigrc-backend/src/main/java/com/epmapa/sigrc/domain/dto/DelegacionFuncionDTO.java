package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record DelegacionFuncionDTO(
    Integer idDelegacion,
    Integer idAsignacionOrigen,
    Integer idEmpleadoOrigen,
    String empleadoOrigen,
    String puestoOrigen,
    String unidadOrigen,
    Integer idAsignacionDelegada,
    Integer idEmpleadoDelegado,
    String empleadoDelegado,
    String puestoDelegado,
    String unidadDelegada,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String tipo,
    String alcance,
    Long documentoRespaldoId,
    String estado,
    String observacion,
    Integer creadoPor
) {}