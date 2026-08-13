package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.NivelOrganizacional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NivelOrganizacionalRepository extends JpaRepository<NivelOrganizacional, Integer> {
    List<NivelOrganizacional> findByActivoTrueOrderByOrdenAsc();
    boolean existsByCodigo(String codigo);
}
