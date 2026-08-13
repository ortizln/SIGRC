package com.epmapa.sigrc.domain.dto;

import com.epmapa.sigrc.domain.entity.EmpleadoCapacitacion;
import com.epmapa.sigrc.domain.entity.EmpleadoDocumento;
import com.epmapa.sigrc.domain.entity.EmpleadoExperiencia;
import com.epmapa.sigrc.domain.entity.EmpleadoFormacion;

import java.time.LocalDate;
import java.util.List;

public record EmpleadoRequest(
    String tipoIdentificacion,
    String identificacion,
    String nombres,
    String apellidos,
    LocalDate fechaNacimiento,
    String sexo,
    String estadoCivil,
    String correoPersonal,
    String correoInstitucional,
    String telefono,
    String celular,
    String direccion,
    String fotoUrl,
    String tipoPersonal,
    String estadoLaboral,
    LocalDate fechaIngresoInstitucion,
    LocalDate fechaSalidaInstitucion,
    String observaciones,
    List<EmpleadoFormacion> formaciones,
    List<EmpleadoExperiencia> experiencias,
    List<EmpleadoCapacitacion> capacitaciones,
    List<EmpleadoDocumento> documentos
) {}
