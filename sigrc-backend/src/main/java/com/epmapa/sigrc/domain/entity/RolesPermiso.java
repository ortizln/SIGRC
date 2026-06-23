package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles_permisos", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolesPermiso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRolPermiso;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso;
}
