package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.AccionPersonal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccionPersonalRepository extends JpaRepository<AccionPersonal, Integer> {
    boolean existsByNumero(String numero);
    List<AccionPersonal> findByEmpleadoIdEmpleadoOrderByCreatedAtDesc(Integer idEmpleado);
    List<AccionPersonal> findAllByOrderByCreatedAtDesc();
}