package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record CorrespondenciaDerivarRequest(
    String sumilla,
    List<DestinoDerivacionDTO> destinos
) {}
