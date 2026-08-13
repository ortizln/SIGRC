package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puesto_capacitacion", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PuestoCapacitacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCapacitacion;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(length = 500)
    private String descripcion;
    @Column(name = "horas_requeridas")
    private Integer horasRequeridas;
    @Builder.Default
    private Boolean obligatorio = true;
}
