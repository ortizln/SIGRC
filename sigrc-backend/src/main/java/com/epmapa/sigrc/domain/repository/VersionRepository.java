package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<Version, Integer> {
    List<Version> findBySistemaIdSistemaOrderByCreadoEnDesc(Integer idSistema);
    Optional<Version> findTopBySistemaIdSistemaOrderByCreadoEnDesc(Integer idSistema);
    List<Version> findByEstado(String estado);
}
