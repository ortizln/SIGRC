package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrespondenciaRespuestaRepository extends JpaRepository<CorrespondenciaRespuesta, Integer> {
    List<CorrespondenciaRespuesta> findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnAsc(Integer idCorrespondencia);
}
