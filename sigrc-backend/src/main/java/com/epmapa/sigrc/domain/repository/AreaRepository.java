package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    List<Area> findByActivoTrueOrderByNombre();
}
