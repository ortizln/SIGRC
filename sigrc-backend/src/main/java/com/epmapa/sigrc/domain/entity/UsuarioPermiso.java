package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios_permisos", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsuarioPermiso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuarioPermiso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 20)
    private String tipoAcceso;

    @Builder.Default
    private Boolean activo = true;
}
