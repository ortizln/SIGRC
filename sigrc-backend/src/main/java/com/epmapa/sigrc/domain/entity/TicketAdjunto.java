package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_adjuntos", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketAdjunto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAdjunto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comentario")
    private TicketComentario comentario;
    @Column(nullable = false, length = 255)
    private String nombreOriginal;
    @Column(nullable = false, length = 255)
    private String nombreArchivo;
    @Column(nullable = false, length = 500)
    private String rutaArchivo;
    @Column(nullable = false, length = 100)
    private String tipoMime;
    @Column(nullable = false)
    private Long tamanoBytes;
    @Column(nullable = false, length = 64)
    private String hashSha256;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
