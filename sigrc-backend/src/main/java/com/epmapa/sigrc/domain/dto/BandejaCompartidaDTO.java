package com.epmapa.sigrc.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BandejaCompartidaDTO(
    Integer idVinculo,
    Integer puestoTitularId,
    String puestoTitularNombre,
    String nombreTitular,
    List<CorrespondenciaDTO> documentos,
    long totalNoLeidos
) {}
