package com.epmapa.sigrc.domain.dto;

import java.time.LocalDate;

public record EmpleadoDTO(
    Integer idEmpleado,
    String tipoIdentificacion,
    String identificacion,
    String nombres,
    String apellidos,
    String nombreCompleto,
    LocalDate fechaNacimiento,
    String sexo,
    String estadoCivil,
    String correoPersonal,
    String correoInstitucional,
    String telefono,
    String celular,
    String direccion,
    String tipoPersonal,
    String estadoLaboral,
    LocalDate fechaIngresoInstitucion,
    LocalDate fechaSalidaInstitucion,
    Boolean activo,
    Integer idUsuario
) {}
