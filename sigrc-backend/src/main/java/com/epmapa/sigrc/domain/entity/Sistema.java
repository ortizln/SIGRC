package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "sistemas", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sistema {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSistema;
    @Column(unique = true, nullable = false, length = 30)
    private String codigo;
    @Column(nullable = false, length = 200)
    private String nombre;
    private String descripcion;
    private String versionActual;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;
    private String tecnologia;
    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
