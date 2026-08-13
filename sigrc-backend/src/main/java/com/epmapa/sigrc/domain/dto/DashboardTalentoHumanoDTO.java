package com.epmapa.sigrc.domain.dto;

import java.util.List;

public record DashboardTalentoHumanoDTO(
    long totalEmpleados,
    long activos,
    long desvinculados,
    long puestosOcupados,
    long puestosVacantes,
    long personalVacaciones,
    long personalLicencia,
    long capacitacionesRegistradas,
    long movimientosDelMes,
    List<ItemCount> porUnidad,
    List<ItemCount> porPuesto,
    List<ItemCount> porGrupoOcupacional,
    List<ItemCount> porTipoPersonal,
    List<ItemCount> porEstadoLaboral
) {
    public record ItemCount(String label, long cantidad) {}
}