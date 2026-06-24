package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrespondenciaAdjuntoRepository extends JpaRepository<CorrespondenciaAdjunto, Integer> {
    List<CorrespondenciaAdjunto> findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(Integer idCorrespondencia);
}
