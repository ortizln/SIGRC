package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delegacion_funcion", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DelegacionFuncion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDelegacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_origen_id", nullable = false)
    private AsignacionPuesto asignacionOrigen;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_delegada_id", nullable = false)
    private AsignacionPuesto asignacionDelegada;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    @Column(length = 30)
    private String tipo;
    @Column(length = 20)
    private String alcance;
    @Column(name = "documento_respaldo_id")
    private Long documentoRespaldoId;
    @Column(nullable = false, length = 20)
    private String estado;
    @Column(length = 1000)
    private String observacion;
    @Column(name = "creado_por")
    private Integer creadoPor;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
