package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByActivoTrueOrderByNombre();

    @Modifying
    @Query(value = "DELETE FROM sigrc.categorias WHERE id_categoria = :id", nativeQuery = true)
    int hardDelete(Integer id);
}
