package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "empleado_capacitacion", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoCapacitacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCapacitacion;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(length = 200)
    private String institucion;
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    private Integer horas;
    @Column(length = 30)
    private String tipo;
    @Column(name = "certificado_documento_id")
    private Integer certificadoDocumentoId;
}
