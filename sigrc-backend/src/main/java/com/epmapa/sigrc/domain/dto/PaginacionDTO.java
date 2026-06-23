package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record PaginacionDTO<T>(
    List<T> contenido,
    int pagina,
    int tamanio,
    long totalElementos,
    int totalPaginas,
    boolean primera,
    boolean ultima
) {}
