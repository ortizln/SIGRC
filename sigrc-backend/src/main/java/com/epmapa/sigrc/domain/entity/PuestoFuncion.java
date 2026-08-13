package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puesto_funcion", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PuestoFuncion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idFuncion;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;
    @Column(nullable = false, length = 1000)
    private String descripcion;
    @Column(length = 20)
    private String tipo;
    private Integer orden;
    @Builder.Default
    private Boolean activo = true;
}
