package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotificacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_destinatario", nullable = false)
    private Usuario destinatario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket")
    private Ticket ticket;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Column(nullable = false, length = 300)
    private String asunto;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;
    @Builder.Default
    private Boolean leido = false;
    private LocalDateTime fechaLectura;
    @Builder.Default
    private Boolean enviadoCorreo = false;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
