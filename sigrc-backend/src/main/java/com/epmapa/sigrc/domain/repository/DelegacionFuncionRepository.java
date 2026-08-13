package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.DelegacionFuncion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelegacionFuncionRepository extends JpaRepository<DelegacionFuncion, Integer> {
    List<DelegacionFuncion> findAllByOrderByFechaInicioDesc();
    List<DelegacionFuncion> findByAsignacionOrigenIdAsignacionAndEstadoOrderByFechaInicioDesc(Integer idAsignacion, String estado);
    List<DelegacionFuncion> findByAsignacionDelegadaIdAsignacionAndEstadoOrderByFechaInicioDesc(Integer idAsignacion, String estado);
}
