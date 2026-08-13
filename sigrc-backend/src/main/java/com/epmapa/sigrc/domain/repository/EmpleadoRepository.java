package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {
    List<Empleado> findByActivoTrueOrderByApellidosAsc();
    boolean existsByIdentificacion(String identificacion);
    Optional<Empleado> findByIdentificacion(String identificacion);

    @Query("SELECT COUNT(c) FROM EmpleadoCapacitacion c")
    long countCapacitaciones();
}
