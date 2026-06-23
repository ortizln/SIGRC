package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Integer> {
    List<Subcategoria> findByCategoriaIdCategoriaAndActivoTrue(Integer idCategoria);
}
