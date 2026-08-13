package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.AsignacionPuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionPuestoRepository extends JpaRepository<AsignacionPuesto, Integer> {
    List<AsignacionPuesto> findByEmpleadoIdEmpleadoOrderByFechaInicioDesc(Integer idEmpleado);

    Optional<AsignacionPuesto> findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
        Integer idEmpleado, String estado);

    Optional<AsignacionPuesto> findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueOrderByFechaInicioDesc(
        Integer idUnidad, String estado);

    Optional<AsignacionPuesto> findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsResponsableUnidadTrueOrderByFechaInicioDesc(
        Integer idUnidad, String estado);

    Optional<AsignacionPuesto> findFirstByUnidadOrganizacionalIdUnidadAndEstadoAndEsPrincipalTrueAndPuestoEsJefaturaTrueOrderByFechaInicioDesc(
        Integer idUnidad, String estado);

    boolean existsByEmpleadoIdEmpleadoAndEstado(Integer idEmpleado, String estado);

    void deleteByEmpleadoIdEmpleadoAndEsPrincipalFalseAndEstado(Integer idEmpleado, String estado);

    List<AsignacionPuesto> findByEstadoAndEsPrincipalTrueAndPuestoActivoTrueOrderByFechaInicioDesc(String estado);
}
