package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "correspondencia", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Correspondencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCorrespondencia;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroInterno;

    @Column(length = 100)
    private String codigoDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    private CorrespondenciaDocumentoTipo tipoDocumento;

    @Column(nullable = false, length = 300)
    private String asunto;

    @Column(columnDefinition = "TEXT")
    private String resumenEjecutivo;

    @Column(nullable = false)
    private LocalDate fechaDocumento;

    @Column(nullable = false)
    private LocalDate fechaRecepcion;

    @Column(nullable = false, length = 5)
    private String horaRecepcion;

    @Column(nullable = false, length = 150)
    private String personaEntrega;

    @Column(length = 100)
    private String cargo;

    @Column(length = 150)
    private String institucion;

    @Column(length = 150)
    private String departamentoRemitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    private Usuario responsable;

    @Column(nullable = false, length = 10)
    private String prioridad;

    @Column(nullable = false, length = 30)
    private String estado;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String sentido = "INGRESO";

    @Builder.Default
    @Column(nullable = false)
    private Boolean requiereRespuesta = false;

    private LocalDate fechaLimiteRespuesta;

    @Builder.Default
    @Column(nullable = false)
    private Boolean generaTicket = false;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @ManyToMany
    @JoinTable(
        name = "correspondencia_referencia",
        schema = "sigrc",
        joinColumns = @JoinColumn(name = "id_correspondencia"),
        inverseJoinColumns = @JoinColumn(name = "id_referencia")
    )
    @Builder.Default
    private List<Correspondencia> referencias = new ArrayList<>();

    @OneToMany(mappedBy = "correspondencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CorrespondenciaDestinatario> destinatarios = new ArrayList<>();
}
