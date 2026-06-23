package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAuditoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
    @Column(nullable = false, length = 50)
    private String username;
    @Column(nullable = false)
    private String accion;
    @Column(nullable = false, length = 20)
    private String tipoOperacion;
    private String tablaAfectada;
    private Integer idRegistro;
    private String datosAnteriores;
    private String datosNuevos;
    @Column(nullable = false, length = 45)
    private String direccionIp;
    @Column(length = 500)
    private String userAgent;
    private String sesionId;
    @Column(nullable = false, length = 10)
    private String resultado;
    private String detalle;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
