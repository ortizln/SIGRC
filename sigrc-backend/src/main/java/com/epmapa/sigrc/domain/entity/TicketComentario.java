package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_comentarios", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketComentario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idComentario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String comentario;
    @Builder.Default
    private Boolean esInterno = false;
    @Builder.Default
    private Boolean editado = false;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
