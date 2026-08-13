package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "empleado_experiencia", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoExperiencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idExperiencia;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(length = 200)
    private String institucion;
    @Column(length = 200)
    private String cargo;
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    @Column(length = 1000)
    private String descripcion;
    @Column(name = "documento_id")
    private Integer documentoId;
}
