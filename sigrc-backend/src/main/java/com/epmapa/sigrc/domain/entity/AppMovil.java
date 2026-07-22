package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_movil", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppMovil {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAppMovil;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 100)
    private String nombreArchivo;

    @Column(nullable = false)
    private String rutaArchivo;

    @Column(nullable = false)
    private Long tamanioBytes;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 64)
    private String checksum;

    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
