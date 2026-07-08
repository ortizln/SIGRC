package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaDestinatario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrespondenciaDestinatarioRepository extends JpaRepository<CorrespondenciaDestinatario, Integer> {
    List<CorrespondenciaDestinatario> findByCorrespondenciaIdCorrespondencia(Integer idCorrespondencia);
    void deleteByCorrespondenciaIdCorrespondencia(Integer idCorrespondencia);
}
