package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accion_personal", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccionPersonal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAccion;
    @Column(nullable = false, unique = true, length = 30)
    private String numero;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(nullable = false, length = 40)
    private String tipo;
    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;
    @Column(name = "fecha_vigencia_desde")
    private LocalDate fechaVigenciaDesde;
    @Column(name = "fecha_vigencia_hasta")
    private LocalDate fechaVigenciaHasta;
    @Column(length = 1000)
    private String motivo;
    @Column(name = "situacion_actual", length = 1000)
    private String situacionActual;
    @Column(name = "situacion_propuesta", length = 1000)
    private String situacionPropuesta;
    @Column(name = "documento_id")
    private Integer documentoId;
    @Column(nullable = false, length = 20)
    private String estado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elaborado_por", nullable = false)
    private Usuario elaboradoPor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por")
    private Usuario revisadoPor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}