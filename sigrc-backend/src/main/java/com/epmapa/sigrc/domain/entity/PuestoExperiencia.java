package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puesto_experiencia", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PuestoExperiencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idExperiencia;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;
    @Column(name = "tiempo_meses")
    private Integer tiempoMeses;
    @Column(length = 500)
    private String especificidad;
    @Builder.Default
    private Boolean obligatorio = true;
}
