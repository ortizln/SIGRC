package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "puesto", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Puesto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPuesto;
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;
    @Column(nullable = false, length = 200)
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_organizacional_id")
    private UnidadOrganizacional unidadOrganizacional;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_manual_id")
    private VersionManual versionManual;
    @Column(name = "rol_funcional", length = 100)
    private String rolFuncional;
    private String eje;
    @Column(name = "grupo_ocupacional", length = 20)
    private String grupoOcupacional;
    @Column(length = 2000)
    private String objetivo;
    @Column(name = "nivel_instruccion", length = 100)
    private String nivelInstruccion;
    @Column(name = "experiencia_meses")
    private Integer experienciaMeses;
    @Builder.Default
    @Column(name = "es_jefatura")
    private Boolean esJefatura = false;
    @Builder.Default
    @Column(name = "es_responsable_unidad")
    private Boolean esResponsableUnidad = false;
    @Column(name = "numero_plazas")
    private Integer numeroPlazas;
    @Builder.Default
    private Boolean activo = true;
    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;
    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;
    private Integer version;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Builder.Default
    @OneToMany(mappedBy = "puesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PuestoFuncion> funciones = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "puesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PuestoFormacion> formaciones = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "puesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PuestoExperiencia> experiencias = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "puesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PuestoCapacitacion> capacitaciones = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "puesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PuestoProducto> productos = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "puesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PuestoInterfaz> interfaces = new ArrayList<>();
}
