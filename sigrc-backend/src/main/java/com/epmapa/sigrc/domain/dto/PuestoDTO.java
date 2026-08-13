package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record PuestoDTO(
    Integer idPuesto,
    String codigo,
    String nombre,
    Integer idUnidad,
    String unidadNombre,
    String rolFuncional,
    String eje,
    String grupoOcupacional,
    String nivelInstruccion,
    Integer experienciaMeses,
    Boolean esJefatura,
    Boolean esResponsableUnidad,
    Integer numeroPlazas,
    Boolean activo,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta,
    Integer version,
    Integer idVersionManual
) {}
