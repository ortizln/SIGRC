package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "empleado", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmpleado;
    @Column(name = "tipo_identificacion", length = 20)
    private String tipoIdentificacion;
    @Column(unique = true, nullable = false, length = 20)
    private String identificacion;
    @Column(nullable = false, length = 100)
    private String nombres;
    @Column(nullable = false, length = 100)
    private String apellidos;
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    @Column(length = 10)
    private String sexo;
    @Column(name = "estado_civil", length = 20)
    private String estadoCivil;
    @Column(name = "correo_personal", length = 150)
    private String correoPersonal;
    @Column(name = "correo_institucional", length = 150)
    private String correoInstitucional;
    private String telefono;
    private String celular;
    private String direccion;
    @Column(name = "foto_url")
    private String fotoUrl;
    @Column(name = "tipo_personal", length = 30)
    private String tipoPersonal;
    @Column(name = "estado_laboral", length = 30)
    private String estadoLaboral;
    @Column(name = "fecha_ingreso_institucion")
    private LocalDate fechaIngresoInstitucion;
    @Column(name = "fecha_salida_institucion")
    private LocalDate fechaSalidaInstitucion;
    @Column(length = 2000)
    private String observaciones;
    @Builder.Default
    private Boolean activo = true;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmpleadoFormacion> formaciones = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmpleadoExperiencia> experiencias = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmpleadoCapacitacion> capacitaciones = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmpleadoDocumento> documentos = new ArrayList<>();
}
