package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "unidad_organizacional", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnidadOrganizacional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUnidad;
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(length = 20)
    private String sigla;
    private String descripcion;
    @Column(length = 30)
    private String tipoUnidad;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_organizacional_id")
    private NivelOrganizacional nivelOrganizacional;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_padre_id")
    private UnidadOrganizacional unidadPadre;
    @Column(name = "responsable_asignacion_id")
    private Integer responsableAsignacionId;
    private Integer orden;
    @Builder.Default
    private Boolean activo = true;
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
