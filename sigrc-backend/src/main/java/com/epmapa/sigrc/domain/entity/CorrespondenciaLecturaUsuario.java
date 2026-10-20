package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * BCC — Lectura personal de un documento por un usuario específico.
 * Permite que la secretaria y el gerente tengan estados de lectura
 * independientes sobre el mismo documento.
 */
@Entity
@Table(name = "correspondencia_lectura_usuario", schema = "sigrc",
       uniqueConstraints = @UniqueConstraint(columnNames = {"correspondencia_id", "usuario_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaLecturaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idLectura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correspondencia_id", nullable = false)
    private Correspondencia correspondencia;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @CreationTimestamp
    @Column(name = "leido_en", nullable = false, updatable = false)
    private LocalDateTime leidoEn;
}
