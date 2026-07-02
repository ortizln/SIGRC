package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Integer> {
    List<Subcategoria> findByCategoriaIdCategoriaAndActivoTrue(Integer idCategoria);

    @Modifying
    @Query(value = "DELETE FROM sigrc.subcategorias WHERE id_subcategoria = :id", nativeQuery = true)
    int hardDelete(Integer id);
}
