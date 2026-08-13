package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "version_manual", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionManual {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVersionManual;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(nullable = false, length = 20)
    private String version;
    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;
    @Column(name = "fecha_vigencia")
    private LocalDate fechaVigencia;
    @Column(name = "documento_id")
    private Long documentoId;
    @Column(nullable = false, length = 20)
    private String estado;
    @Column(length = 1000)
    private String observaciones;
    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
