package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "slas", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sla {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSla;
    @Column(unique = true, nullable = false, length = 30)
    private String codigo;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(nullable = false, length = 20)
    private String tipoTicket;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
    @Column(nullable = false, length = 15)
    private String prioridad;
    @Column(nullable = false)
    private Integer tiempoRespuestaHoras;
    @Column(nullable = false)
    private Integer tiempoSolucionHoras;
    private Boolean activo = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
