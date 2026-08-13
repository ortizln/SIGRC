package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.SolicitudAusencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SolicitudAusenciaRepository extends JpaRepository<SolicitudAusencia, Integer> {
    List<SolicitudAusencia> findByEmpleadoIdEmpleadoOrderByCreatedAtDesc(Integer idEmpleado);
    List<SolicitudAusencia> findAllByOrderByCreatedAtDesc();
    List<SolicitudAusencia> findByEmpleadoIdEmpleadoAndEstadoNotOrderByCreatedAtDesc(Integer idEmpleado, String estado);
    long countByEmpleadoIdEmpleadoAndTipoAndFechaDesdeGreaterThanEqual(Integer idEmpleado, String tipo, LocalDate desde);

    @Query("SELECT s FROM SolicitudAusencia s WHERE s.estado = 'APROBADA' AND s.tipo IN :tipos AND s.fechaDesde <= :hoy AND s.fechaHasta >= :hoy")
    List<SolicitudAusencia> findAprobadasVigentes(@Param("tipos") List<String> tipos, @Param("hoy") LocalDate hoy);
}