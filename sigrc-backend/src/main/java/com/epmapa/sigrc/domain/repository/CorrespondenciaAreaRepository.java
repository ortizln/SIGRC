package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaArea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrespondenciaAreaRepository extends JpaRepository<CorrespondenciaArea, Integer> {
    List<CorrespondenciaArea> findByCorrespondenciaIdCorrespondencia(Integer idCorrespondencia);
    void deleteByCorrespondenciaIdCorrespondencia(Integer idCorrespondencia);
}
