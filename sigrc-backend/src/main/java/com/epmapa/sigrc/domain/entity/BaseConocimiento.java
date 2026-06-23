package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "base_conocimiento", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BaseConocimiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idArticulo;
    @Column(nullable = false, length = 300)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;
    @Column(nullable = false, length = 20)
    private String tipo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;
    private String palabrasClave;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autor", nullable = false)
    private Usuario autor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_revisor")
    private Usuario revisor;
    private Integer version = 1;
    @Column(nullable = false, length = 20)
    private String estado = "BORRADOR";
    private String contenidoHtml;
    private String adjuntos;
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
