package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puesto_formacion", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PuestoFormacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idFormacion;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;
    @Column(name = "nivel_instruccion", length = 100)
    private String nivelInstruccion;
    @Column(name = "titulo_area", length = 200)
    private String tituloArea;
    @Column(length = 500)
    private String detalle;
    @Builder.Default
    private Boolean obligatorio = true;
}
