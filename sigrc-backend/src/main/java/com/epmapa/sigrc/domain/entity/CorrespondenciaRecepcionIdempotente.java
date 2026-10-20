package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * BCC — Clave de idempotencia para recepción delegada.
 * Evita duplicados cuando un asistente reintenta la operación.
 */
@Entity
@Table(name = "correspondencia_recepcion_idempotente", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrespondenciaRecepcionIdempotente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idClave;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correspondencia_id", nullable = false)
    private Integer correspondenciaId;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "puesto_titular_id", nullable = false)
    private Integer puestoTitularId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
