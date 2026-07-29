package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<Version, Integer> {
    List<Version> findBySistemaIdSistemaOrderByCreadoEnDesc(Integer idSistema);
    Optional<Version> findTopBySistemaIdSistemaOrderByCreadoEnDesc(Integer idSistema);
    List<Version> findByEstado(String estado);

    @Query("SELECT v FROM Version v WHERE v.activo = true AND v.estado = 'DESPLEGADO' ORDER BY v.fechaDespliegue DESC")
    Optional<Version> findTopByActivoTrueAndEstadoDesplegadoOrderByFechaDespliegueDesc();

    @Query("SELECT v.estado, COUNT(v) FROM Version v WHERE v.activo = true GROUP BY v.estado")
    List<Object[]> countByEstadoGroup();
}
