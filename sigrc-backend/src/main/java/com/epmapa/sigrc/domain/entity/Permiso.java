package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "permisos", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permiso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPermiso;
    @Column(unique = true, nullable = false, length = 60)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(nullable = false, length = 50)
    private String modulo;
    @Column(nullable = false, length = 20)
    private String tipoAcceso;
    private String descripcion;
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
