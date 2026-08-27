package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;

public interface CorrespondenciaHistorialRepository extends JpaRepository<CorrespondenciaHistorial, Integer> {
    List<CorrespondenciaHistorial> findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnDesc(Integer idCorrespondencia);

    @Query("SELECT h.idDelegacion, COUNT(DISTINCT h.correspondencia.idCorrespondencia) " +
           "FROM CorrespondenciaHistorial h WHERE h.idDelegacion IS NOT NULL GROUP BY h.idDelegacion")
    List<Object[]> countDocumentosPorDelegacion();
}
