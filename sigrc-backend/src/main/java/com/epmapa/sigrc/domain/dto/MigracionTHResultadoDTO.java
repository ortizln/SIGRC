package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record MigracionTHResultadoDTO(
    boolean dryRun,
    int usuariosProcesados,
    int empleadosCreados,
    int asignacionesCreadas,
    int unidadesCreadas,
    int puestosCreados,
    int yaVinculados,
    int conErrores,
    List<DetalleMigracionDTO> detalles
) {

    public record DetalleMigracionDTO(
        Integer idUsuario,
        String username,
        String resultado,
        String detalle,
        Integer idEmpleado,
        Integer idAsignacion
    ) {}
}
