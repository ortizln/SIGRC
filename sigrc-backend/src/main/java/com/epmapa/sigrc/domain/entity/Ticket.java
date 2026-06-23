package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTicket;
    @Column(unique = true, nullable = false, length = 15)
    private String numeroTicket;
    @Column(nullable = false, length = 20)
    private String tipo;
    @Column(nullable = false, length = 20)
    private String estado = "NUEVO";
    @Column(nullable = false, length = 15)
    private String prioridad;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private Usuario solicitante;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area", nullable = false)
    private Area area;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_subcategoria")
    private Subcategoria subcategoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    private Usuario responsable;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sla")
    private Sla sla;
    @Column(nullable = false, length = 300)
    private String asunto;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    private String impacto;
    private String urgencia;
    private String origen = "SISTEMA";
    private LocalDateTime fechaLimite;
    private LocalDateTime fechaCierre;
    private String causaRaiz;
    private String solucion;
    private Boolean esReabierto = false;
    private Integer numeroReaperturas = 0;
    private Integer calificacion;
    private String comentarioCierre;
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
