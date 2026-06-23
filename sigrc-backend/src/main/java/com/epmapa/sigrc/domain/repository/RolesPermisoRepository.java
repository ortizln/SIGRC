package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.RolesPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolesPermisoRepository extends JpaRepository<RolesPermiso, Integer> {
    List<RolesPermiso> findByRolIdRol(Integer idRol);
    void deleteByRolIdRol(Integer idRol);
}
