package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.BaseConocimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseConocimientoRepository extends JpaRepository<BaseConocimiento, Integer> {
    List<BaseConocimiento> findByTipoAndActivoTrue(String tipo);
    List<BaseConocimiento> findByEstadoAndActivoTrue(String estado);
}
