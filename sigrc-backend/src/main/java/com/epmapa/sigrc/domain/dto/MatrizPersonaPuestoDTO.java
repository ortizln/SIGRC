package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record MatrizPersonaPuestoDTO(
    Integer idEmpleado,
    String funcionario,
    Integer idPuesto,
    String puesto,
    String unidad,
    String grupoOcupacional,
    List<CriterioMatrizDTO> criterios,
    long cumplidos,
    long parciales,
    long noCumplidos
) {
    public record CriterioMatrizDTO(
        String criterio,
        String requerido,
        String encontrado,
        String estado // CUMPLE | PARCIAL | NO_CUMPLE
    ) {}
}