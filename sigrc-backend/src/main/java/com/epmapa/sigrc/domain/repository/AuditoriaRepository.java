package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
    Page<Auditoria> findByCreadoEnBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    Page<Auditoria> findByUsername(String username, Pageable pageable);
    Page<Auditoria> findByTablaAfectada(String tabla, Pageable pageable);
    Page<Auditoria> findByTipoOperacion(String tipoOperacion, Pageable pageable);
}
