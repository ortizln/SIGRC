package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record VinculoAsistenteRequest(
    Integer puestoAsistenteId,
    Integer puestoTitularId,
    Boolean puedeRecepcionar,
    Boolean puedeOrganizar,
    Boolean puedeSeguimiento,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta
) {}
