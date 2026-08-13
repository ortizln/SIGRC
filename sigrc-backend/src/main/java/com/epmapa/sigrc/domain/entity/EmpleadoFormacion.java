package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "empleado_formacion", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoFormacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idFormacion;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(length = 50)
    private String nivel;
    @Column(length = 200)
    private String titulo;
    @Column(length = 200)
    private String institucion;
    @Column(length = 50)
    private String pais;
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    @Column(name = "registro_senescyt", length = 50)
    private String registroSenescyt;
    @Column(name = "documento_id")
    private Integer documentoId;
    @Builder.Default
    private Boolean verificado = false;
}
