package com.epmapa.sigrc.domain.dto;

/**
 * Destino de derivación institucional.
 * tipo: USUARIO | PUESTO | UNIDAD | RESPONSABLE_UNIDAD | JEFE_INMEDIATO
 * idDestino: id de usuario/puesto/unidad según el tipo (null para JEFE_INMEDIATO).
 */
public record DestinoDerivacionDTO(
    String tipo,
    Integer idDestino
) {}
