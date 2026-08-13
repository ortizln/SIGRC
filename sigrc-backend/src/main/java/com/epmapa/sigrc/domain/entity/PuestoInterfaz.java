package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puesto_interfaz", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PuestoInterfaz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idInterfaz;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;
    @Column(name = "unidad_relacionada_id")
    private Integer unidadRelacionadaId;
    @Column(length = 500)
    private String descripcion;
    @Column(name = "tipo_interfaz", length = 30)
    private String tipoInterfaz;
}
