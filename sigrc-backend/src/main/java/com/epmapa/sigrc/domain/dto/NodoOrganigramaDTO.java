package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record NodoOrganigramaDTO(
    Integer idUnidad,
    String codigo,
    String nombre,
    String sigla,
    String tipoUnidad,
    String nivelNombre,
    Integer orden,
    Boolean activo,
    String responsable,
    String puestoResponsable,
    Integer plazas,
    Integer plazasOcupadas,
    Integer vacantes,
    List<NodoOrganigramaDTO> hijos
) {}
