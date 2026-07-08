package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.UsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, Integer> {
    List<UsuarioPermiso> findByUsuarioIdUsuarioAndActivoTrue(Integer idUsuario);
    Optional<UsuarioPermiso> findByUsuarioIdUsuarioAndModuloAndActivoTrue(Integer idUsuario, String modulo);
    void deleteByUsuarioIdUsuario(Integer idUsuario);
}
