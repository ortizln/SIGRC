package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record CorrespondenciaRecepcionRequest(
    String sumilla,
    List<Integer> idsUsuariosDerivados
) {}