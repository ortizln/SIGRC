package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "correspondencia_responsable", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaResponsable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_correspondencia", nullable = false)
    private Correspondencia correspondencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(columnDefinition = "TEXT")
    private String sumilla;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
