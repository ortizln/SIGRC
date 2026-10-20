package com.epmapa.sigrc.domain.dto;

public record VinculoAsistenteDTO(
    Integer idVinculo,
    Integer puestoAsistenteId,
    String puestoAsistenteNombre,
    String nombreAsistente,
    Integer puestoTitularId,
    String puestoTitularNombre,
    String nombreTitular,
    Boolean puedeRecepcionar,
    Boolean puedeOrganizar,
    Boolean puedeSeguimiento,
    String vigenteDesde,
    String vigenteHasta,
    Boolean activo,
    Boolean vigente,
    Integer autorizadoPor,
    String autorizadoPorNombre
) {}
