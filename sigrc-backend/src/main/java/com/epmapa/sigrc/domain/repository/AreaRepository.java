package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    List<Area> findByActivoTrueOrderByNombre();

    @Modifying
    @Query(value = "DELETE FROM sigrc.areas WHERE id_area = :id", nativeQuery = true)
    int hardDelete(Integer id);
}
