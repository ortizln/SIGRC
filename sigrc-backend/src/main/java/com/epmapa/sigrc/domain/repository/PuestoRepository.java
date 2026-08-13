package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Puesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PuestoRepository extends JpaRepository<Puesto, Integer> {
    List<Puesto> findByActivoTrueOrderByNombre();
    boolean existsByCodigo(String codigo);

    @Query("SELECT p FROM Puesto p WHERE p.activo = true AND " +
           "NOT EXISTS (SELECT a FROM AsignacionPuesto a WHERE a.puesto = p AND a.estado = 'ACTIVA' AND a.esPrincipal = true)")
    List<Puesto> findVacantes();
}
