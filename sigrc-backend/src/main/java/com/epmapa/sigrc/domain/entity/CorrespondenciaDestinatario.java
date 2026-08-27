package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Builder.Default
    private Boolean recibido = false;

    private LocalDateTime fechaRecibido;

    @Builder.Default
    private Boolean leido = false;

    private LocalDateTime fechaLeido;

    @Column(columnDefinition = "TEXT")
    private String sumilla;

    @Column
    private Integer idDelegacion;

    @Column
    private Integer usuarioOriginal;
}
