package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrespondenciaHistorialRepository extends JpaRepository<CorrespondenciaHistorial, Integer> {
    List<CorrespondenciaHistorial> findByCorrespondenciaIdCorrespondenciaOrderByCreadoEnDesc(Integer idCorrespondencia);
}
