package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "nivel_organizacional", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NivelOrganizacional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNivel;
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String nombre;
    private String descripcion;
    private Integer orden;
    @Builder.Default
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
