package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_ausencia", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudAusencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSolicitud;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;
    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;
    private Integer dias;
    private Integer horas;
    @Column(length = 1000)
    private String motivo;
    @Column(name = "documento_respaldo_id")
    private Integer documentoRespaldoId;
    @Column(nullable = false, length = 20)
    private String estado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jefe_aprobador_id")
    private Usuario jefeAprobador;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "th_aprobador_id")
    private Usuario thAprobador;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}