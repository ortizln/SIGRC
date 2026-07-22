package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.AppMovil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppMovilRepository extends JpaRepository<AppMovil, Integer> {
    List<AppMovil> findByActivoTrueOrderByCreadoEnDesc();
}
