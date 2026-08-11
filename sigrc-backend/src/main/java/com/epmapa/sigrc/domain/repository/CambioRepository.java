package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Cambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CambioRepository extends JpaRepository<Cambio, Integer> {
    List<Cambio> findByEstado(String estado);
    List<Cambio> findBySistemaIdSistema(Integer idSistema);
    List<Cambio> findByTicketIdTicket(Integer idTicket);

    @Query("SELECT c.estado, COUNT(c) FROM Cambio c WHERE c.activo = true GROUP BY c.estado")
    List<Object[]> countByEstadoGroup();

    @Query("SELECT c.impacto, COUNT(c) FROM Cambio c WHERE c.activo = true GROUP BY c.impacto")
    List<Object[]> countByImpactoGroup();

    @Query("SELECT COUNT(c) FROM Cambio c WHERE c.activo = true AND c.estado = 'SOLICITADO'")
    long countSolicitados();

    @Query("SELECT COUNT(c) FROM Cambio c WHERE c.activo = true AND c.estado = 'APROBADO'")
    long countAprobados();

    @Query("SELECT COUNT(c) FROM Cambio c WHERE c.activo = true AND c.estado = 'COMPLETADO'")
    long countCompletados();

    @Query("SELECT c FROM Cambio c WHERE c.activo = true ORDER BY c.creadoEn DESC")
    Optional<Cambio> findTopByActivoTrueOrderByCreadoEnDesc();

    // ─── Consultas filtradas por usuario (dashboard personal) ───

    @Query("SELECT COUNT(c) FROM Cambio c WHERE c.activo = true AND c.estado = 'SOLICITADO' AND (c.solicitante.idUsuario = :idUsuario OR c.responsable.idUsuario = :idUsuario OR c.aprobador.idUsuario = :idUsuario)")
    long countSolicitadosPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT COUNT(c) FROM Cambio c WHERE c.activo = true AND c.estado = 'APROBADO' AND (c.solicitante.idUsuario = :idUsuario OR c.responsable.idUsuario = :idUsuario OR c.aprobador.idUsuario = :idUsuario)")
    long countAprobadosPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT COUNT(c) FROM Cambio c WHERE c.activo = true AND c.estado = 'COMPLETADO' AND (c.solicitante.idUsuario = :idUsuario OR c.responsable.idUsuario = :idUsuario OR c.aprobador.idUsuario = :idUsuario)")
    long countCompletadosPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT c.estado, COUNT(c) FROM Cambio c WHERE c.activo = true AND (c.solicitante.idUsuario = :idUsuario OR c.responsable.idUsuario = :idUsuario OR c.aprobador.idUsuario = :idUsuario) GROUP BY c.estado")
    List<Object[]> countByEstadoGroupPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("SELECT c.impacto, COUNT(c) FROM Cambio c WHERE c.activo = true AND (c.solicitante.idUsuario = :idUsuario OR c.responsable.idUsuario = :idUsuario OR c.aprobador.idUsuario = :idUsuario) GROUP BY c.impacto")
    List<Object[]> countByImpactoGroupPorUsuario(@Param("idUsuario") Integer idUsuario);
}
