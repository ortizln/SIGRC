package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BCC — Vínculo administrativo entre un puesto asistente (secretaría)
 * y un puesto titular (gerencia). Permite recepción delegada sobre
 * la bandeja oficial del titular sin suplantación de identidad.
 */
@Entity
@Table(name = "correspondencia_vinculo_asistente", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaVinculoAsistente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVinculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_asistente_id", nullable = false)
    private Puesto puestoAsistente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_titular_id", nullable = false)
    private Puesto puestoTitular;

    @Column(name = "nombre_asistente", length = 200)
    private String nombreAsistente;

    @Column(name = "nombre_titular", length = 200)
    private String nombreTitular;

    @Builder.Default
    @Column(name = "puede_recepcionar", nullable = false)
    private Boolean puedeRecepcionar = true;

    @Builder.Default
    @Column(name = "puede_organizar", nullable = false)
    private Boolean puedeOrganizar = true;

    @Builder.Default
    @Column(name = "puede_seguimiento", nullable = false)
    private Boolean puedeSeguimiento = true;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizado_por", nullable = false)
    private Usuario autorizadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public boolean vigente(LocalDate hoy) {
        if (!Boolean.TRUE.equals(activo)) return false;
        if (vigenteDesde != null && hoy.isBefore(vigenteDesde)) return false;
        if (vigenteHasta != null && hoy.isAfter(vigenteHasta)) return false;
        return true;
    }
}
