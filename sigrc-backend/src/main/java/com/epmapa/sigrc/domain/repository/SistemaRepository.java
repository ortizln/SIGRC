package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Sistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SistemaRepository extends JpaRepository<Sistema, Integer> {
    List<Sistema> findByActivoTrueOrderByNombre();

    @Modifying
    @Query(value = "DELETE FROM sigrc.sistemas WHERE id_sistema = :id", nativeQuery = true)
    int hardDelete(Integer id);
}
