package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "versiones", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Version {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVersion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema", nullable = false)
    private Sistema sistema;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cambio")
    private Cambio cambio;
    @Column(nullable = false, length = 20)
    private String version;
    @Column(nullable = false, length = 20)
    private String tipo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    private String notasLiberacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable", nullable = false)
    private Usuario responsable;
    private LocalDateTime fechaDespliegue;
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String ambiente = "PRODUCCION";
    @Builder.Default
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
