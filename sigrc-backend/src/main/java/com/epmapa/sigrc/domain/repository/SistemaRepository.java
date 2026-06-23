package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Sistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SistemaRepository extends JpaRepository<Sistema, Integer> {
    List<Sistema> findByActivoTrueOrderByNombre();
}
