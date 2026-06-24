package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaDocumentoTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrespondenciaDocumentoTipoRepository extends JpaRepository<CorrespondenciaDocumentoTipo, Integer> {
    List<CorrespondenciaDocumentoTipo> findByActivoTrueOrderByNombre();
}
