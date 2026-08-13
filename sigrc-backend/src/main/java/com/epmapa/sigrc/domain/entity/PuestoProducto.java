package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puesto_producto", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PuestoProducto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProducto;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;
    @Column(nullable = false, length = 1000)
    private String descripcion;
    private Integer orden;
    @Builder.Default
    private Boolean activo = true;
}
