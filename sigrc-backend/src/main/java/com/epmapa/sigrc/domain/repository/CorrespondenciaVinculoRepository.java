package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaVinculoAsistente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CorrespondenciaVinculoRepository extends JpaRepository<CorrespondenciaVinculoAsistente, Integer> {

    /**
     * Vínculos vigentes donde el puesto del usuario autenticado es asistente.
     */
    @Query("SELECT v FROM CorrespondenciaVinculoAsistente v " +
           "WHERE v.activo = true " +
           "AND v.puestoAsistente.idPuesto = :idPuestoAsistente " +
           "AND v.vigenteDesde <= :hoy " +
           "AND (v.vigenteHasta IS NULL OR v.vigenteHasta >= :hoy)")
    List<CorrespondenciaVinculoAsistente> findVigentesComoAsistente(
            @Param("idPuestoAsistente") Integer idPuestoAsistente,
            @Param("hoy") LocalDate hoy);

    /**
     * Vínculos vigentes donde el puesto del usuario es titular.
     */
    @Query("SELECT v FROM CorrespondenciaVinculoAsistente v " +
           "WHERE v.activo = true " +
           "AND v.puestoTitular.idPuesto = :idPuestoTitular " +
           "AND v.vigenteDesde <= :hoy " +
           "AND (v.vigenteHasta IS NULL OR v.vigenteHasta >= :hoy)")
    List<CorrespondenciaVinculoAsistente> findVigentesComoTitular(
            @Param("idPuestoTitular") Integer idPuestoTitular,
            @Param("hoy") LocalDate hoy);

    /**
     * Busca un vínculo vigente específico entre dos puestos.
     */
    @Query("SELECT v FROM CorrespondenciaVinculoAsistente v " +
           "WHERE v.activo = true " +
           "AND v.puestoAsistente.idPuesto = :idAsistente " +
           "AND v.puestoTitular.idPuesto = :idTitular " +
           "AND v.vigenteDesde <= :hoy " +
           "AND (v.vigenteHasta IS NULL OR v.vigenteHasta >= :hoy)")
    Optional<CorrespondenciaVinculoAsistente> findVigenteEntre(
            @Param("idAsistente") Integer idAsistente,
            @Param("idTitular") Integer idTitular,
            @Param("hoy") LocalDate hoy);

    boolean existsByPuestoAsistente_IdPuestoAndPuestoTitular_IdPuestoAndActivoTrue(
            Integer idAsistente, Integer idTitular);

    List<CorrespondenciaVinculoAsistente> findByActivoTrueOrderByCreadoEnDesc();

    List<CorrespondenciaVinculoAsistente> findByPuestoAsistente_IdPuestoAndActivoTrue(Integer idPuesto);
}
