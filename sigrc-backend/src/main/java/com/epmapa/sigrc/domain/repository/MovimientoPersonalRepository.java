package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.MovimientoPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoPersonalRepository extends JpaRepository<MovimientoPersonal, Integer> {
    List<MovimientoPersonal> findByEmpleadoIdEmpleadoOrderByCreatedAtDesc(Integer idEmpleado);
    List<MovimientoPersonal> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(m) FROM MovimientoPersonal m WHERE m.createdAt >= :desde")
    long countDesde(@Param("desde") LocalDateTime desde);
}