package com.epmapa.sigrc.domain.dto;

import com.epmapa.sigrc.domain.entity.PuestoCapacitacion;
import com.epmapa.sigrc.domain.entity.PuestoExperiencia;
import com.epmapa.sigrc.domain.entity.PuestoFormacion;
import com.epmapa.sigrc.domain.entity.PuestoFuncion;
import com.epmapa.sigrc.domain.entity.PuestoInterfaz;
import com.epmapa.sigrc.domain.entity.PuestoProducto;

import java.time.LocalDate;
import java.util.List;

public record ManualFuncionesDTO(
    VersionManualDTO version,
    List<DireccionManualDTO> direcciones
) {

    public record DireccionManualDTO(
        Integer idUnidad,
        String nombre,
        String sigla,
        String tipoUnidad,
        String nivelOrganizacional,
        List<UnidadManualDTO> unidades,
        List<PuestoManualDTO> puestos
    ) {}

    public record UnidadManualDTO(
        Integer idUnidad,
        String nombre,
        String sigla,
        String tipoUnidad,
        List<PuestoManualDTO> puestos
    ) {}

    public record PuestoManualDTO(
        Integer idPuesto,
        String codigo,
        String nombre,
        String rolFuncional,
        String eje,
        String grupoOcupacional,
        String objetivo,
        String nivelInstruccion,
        Integer experienciaMeses,
        Boolean esJefatura,
        Boolean esResponsableUnidad,
        Integer numeroPlazas,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        Integer versionPuesto,
        List<PuestoFuncion> funciones,
        List<PuestoFormacion> formaciones,
        List<PuestoExperiencia> experiencias,
        List<PuestoCapacitacion> capacitaciones,
        List<PuestoProducto> productos,
        List<PuestoInterfaz> interfaces
    ) {}
}