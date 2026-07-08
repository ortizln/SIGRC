package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "correspondencia_destinatario", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaDestinatario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCorrespondenciaDestinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_correspondencia", nullable = false)
    private Correspondencia correspondencia;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer idDestinatario;

    @Column(nullable = false, length = 300)
    private String nombre;
}
