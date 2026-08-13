package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.UnidadOrganizacional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnidadOrganizacionalRepository extends JpaRepository<UnidadOrganizacional, Integer> {
    List<UnidadOrganizacional> findByActivoTrueOrderByOrdenAsc();
    List<UnidadOrganizacional> findAllByOrderByOrdenAsc();
    boolean existsByCodigo(String codigo);
    long countByUnidadPadreIdUnidad(Integer idPadre);

    @Query("SELECT u.idUnidad FROM UnidadOrganizacional u WHERE u.idUnidad = :id OR u.unidadPadre.idUnidad = :id")
    List<Integer> findByIdWithHijas(@Param("id") Integer id);
}
