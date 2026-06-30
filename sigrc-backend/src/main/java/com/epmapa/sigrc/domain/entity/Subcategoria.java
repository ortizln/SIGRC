package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "subcategorias", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subcategoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSubcategoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;
    @Column(unique = true, nullable = false, length = 30)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String nombre;
    private String descripcion;
    @Builder.Default
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
