package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asignacion_puesto", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AsignacionPuesto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAsignacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id")
    private Puesto puesto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_organizacional_id")
    private UnidadOrganizacional unidadOrganizacional;
    @Column(name = "tipo_asignacion", length = 30)
    private String tipoAsignacion;
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    @Builder.Default
    @Column(name = "es_principal")
    private Boolean esPrincipal = true;
    @Column(length = 20)
    private String estado;
    @Column(name = "accion_personal_id")
    private Integer accionPersonalId;
    @Column(length = 1000)
    private String observacion;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
