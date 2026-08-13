package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_personal", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoPersonal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMovimiento;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(name = "tipo_movimiento", nullable = false, length = 40)
    private String tipoMovimiento;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_origen_id")
    private AsignacionPuesto asignacionOrigen;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_destino_id")
    private Puesto puestoDestino;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_destino_id")
    private UnidadOrganizacional unidadDestino;
    @Column(name = "fecha_solicitud")
    private LocalDate fechaSolicitud;
    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;
    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;
    @Column(length = 1000)
    private String motivo;
    @Column(name = "documento_respaldo_id")
    private Integer documentoRespaldoId;
    @Column(nullable = false, length = 20)
    private String estado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}