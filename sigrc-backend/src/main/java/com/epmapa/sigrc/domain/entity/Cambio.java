package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "cambios", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cambio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCambio;
    @Column(unique = true, nullable = false, length = 15)
    private String codigoCambio;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket")
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;
    @Column(nullable = false, length = 300)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificacion;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Column(nullable = false, length = 15)
    private String impacto;
    @Column(nullable = false, length = 15)
    private String riesgo;
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String estado = "SOLICITADO";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private Usuario solicitante;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aprobador")
    private Usuario aprobador;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    private Usuario responsable;
    private String planImplementacion;
    private String planRetorno;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaImplementacion;
    private LocalDateTime fechaVerificacion;
    private String resultado;
    private String leccionesAprendidas;
    @Builder.Default
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
