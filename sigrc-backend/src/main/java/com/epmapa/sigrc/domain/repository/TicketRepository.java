package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    Page<Ticket> findByEstadoIn(List<String> estados, Pageable pageable);

    @Query(value = "SELECT t.id_ticket, t.activo, t.asunto, t.calificacion, t.causa_raiz, t.comentario_cierre, t.creado_en, t.descripcion, t.es_reabierto, t.estado, t.fecha_cierre, t.fecha_limite, t.impacto, t.numero_reaperturas, t.numero_ticket, t.origen, t.prioridad, t.solucion, t.tipo, t.urgencia, t.actualizado_en, t.id_area, t.id_categoria, t.id_responsable, t.id_sistema, t.id_sla, t.id_solicitante, t.id_subcategoria FROM sigrc.tickets t WHERE " +
           "(:estado IS NULL OR t.estado = :estado) AND " +
           "(:tipo IS NULL OR t.tipo = :tipo) AND " +
           "(:prioridad IS NULL OR t.prioridad = :prioridad) AND " +
           "(:idSolicitante IS NULL OR t.id_solicitante = :idSolicitante) AND " +
           "(:idResponsable IS NULL OR t.id_responsable = :idResponsable) AND " +
           "(:idArea IS NULL OR t.id_area = :idArea) AND " +
           "(:idSistema IS NULL OR t.id_sistema = :idSistema) AND " +
           "(:texto IS NULL OR LOWER(CAST(t.asunto AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(CAST(t.numero_ticket AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%'))) " +
           "ORDER BY t.creado_en DESC",
           countQuery = "SELECT COUNT(*) FROM sigrc.tickets t WHERE " +
           "(:estado IS NULL OR t.estado = :estado) AND " +
           "(:tipo IS NULL OR t.tipo = :tipo) AND " +
           "(:prioridad IS NULL OR t.prioridad = :prioridad) AND " +
           "(:idSolicitante IS NULL OR t.id_solicitante = :idSolicitante) AND " +
           "(:idResponsable IS NULL OR t.id_responsable = :idResponsable) AND " +
           "(:idArea IS NULL OR t.id_area = :idArea) AND " +
           "(:idSistema IS NULL OR t.id_sistema = :idSistema) AND " +
           "(:texto IS NULL OR LOWER(CAST(t.asunto AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(CAST(t.numero_ticket AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')))",
           nativeQuery = true)
    Page<Ticket> buscar(@Param("estado") String estado,
                        @Param("tipo") String tipo,
                        @Param("prioridad") String prioridad,
                        @Param("idSolicitante") Integer idSolicitante,
                        @Param("idResponsable") Integer idResponsable,
                        @Param("idArea") Integer idArea,
                        @Param("idSistema") Integer idSistema,
                        @Param("texto") String texto,
                        Pageable pageable);

    long countByEstado(String estado);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.activo = true AND t.estado NOT IN ('CERRADO','RECHAZADO') AND t.fechaLimite < CURRENT_TIMESTAMP")
    long countVencidos();

    @Query("SELECT t.estado, COUNT(t) FROM Ticket t WHERE t.activo = true GROUP BY t.estado")
    List<Object[]> countByEstadoGroup();

    @Query("SELECT t.prioridad, COUNT(t) FROM Ticket t WHERE t.activo = true GROUP BY t.prioridad")
    List<Object[]> countByPrioridadGroup();

    @Query("SELECT t.area.nombre, COUNT(t) FROM Ticket t WHERE t.activo = true GROUP BY t.area.nombre ORDER BY COUNT(t) DESC")
    List<Object[]> countByAreaGroup();

    @Query("SELECT t.sistema.nombre, COUNT(t) FROM Ticket t WHERE t.sistema IS NOT NULL AND t.activo = true GROUP BY t.sistema.nombre ORDER BY COUNT(t) DESC")
    List<Object[]> countBySistemaGroup();

    @Query("SELECT t.tipo, COUNT(t) FROM Ticket t WHERE t.activo = true GROUP BY t.tipo")
    List<Object[]> countByTipoGroup();

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (fecha_cierre - creado_en)) / 3600.0), 0) FROM sigrc.tickets WHERE fecha_cierre IS NOT NULL", nativeQuery = true)
    Double avgTiempoAtencionHoras();

    @Query("SELECT FUNCTION('TO_CHAR', t.creadoEn, 'YYYY-MM') as mes, COUNT(t) FROM Ticket t WHERE t.creadoEn >= :desde GROUP BY mes ORDER BY mes")
    List<Object[]> tendenciasMensuales(@Param("desde") LocalDateTime desde);

    @Query("SELECT t FROM Ticket t WHERE t.estado NOT IN ('CERRADO','RECHAZADO') AND t.fechaLimite < CURRENT_TIMESTAMP")
    List<Ticket> findVencidosActivos();

    long countByEstadoAndResponsableIdUsuario(String estado, Integer idResponsable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.estado NOT IN :estados")
    long countByEstadoNotIn(@Param("estados") List<String> estados);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.estado IN :estados")
    long countByEstadoIn(@Param("estados") List<String> estados);
}
