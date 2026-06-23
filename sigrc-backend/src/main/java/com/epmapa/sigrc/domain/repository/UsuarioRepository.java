package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
