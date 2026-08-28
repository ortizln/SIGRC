package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "correspondencia_responsable", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaResponsable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_correspondencia", nullable = false)
    private Correspondencia correspondencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(columnDefinition = "TEXT")
    private String sumilla;

    @Column(name = "puesto_firmante", length = 200)
    private String puestoFirmante;

    @Column(name = "unidad_firmante", length = 200)
    private String unidadFirmante;

    @Column(name = "asignacion_id")
    private Integer asignacionId;

    @Column(name = "id_delegacion")
    private Integer idDelegacion;

    @Column(name = "usuario_original")
    private Integer usuarioOriginal;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
