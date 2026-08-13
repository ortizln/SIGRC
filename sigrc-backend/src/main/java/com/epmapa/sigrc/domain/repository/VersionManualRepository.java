package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.VersionManual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionManualRepository extends JpaRepository<VersionManual, Integer> {
    List<VersionManual> findAllByOrderByCreadoEnDesc();
    Optional<VersionManual> findFirstByEstadoOrderByCreadoEnDesc(String estado);
    List<VersionManual> findByEstadoOrderByCreadoEnDesc(String estado);
}
