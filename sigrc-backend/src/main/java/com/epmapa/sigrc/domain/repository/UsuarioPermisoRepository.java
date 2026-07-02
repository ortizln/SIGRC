package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.UsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, Integer> {
    List<UsuarioPermiso> findByUsuarioIdUsuarioAndActivoTrue(Integer idUsuario);
    void deleteByUsuarioIdUsuario(Integer idUsuario);
}
