package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "correspondencia_area", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaArea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCorrespondenciaArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_correspondencia", nullable = false)
    private Correspondencia correspondencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area", nullable = false)
    private Area area;
}
