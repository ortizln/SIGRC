package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Correspondencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CorrespondenciaRepository extends JpaRepository<Correspondencia, Integer> {

    Optional<Correspondencia> findByNumeroInterno(String numeroInterno);

    @Query("SELECT c FROM Correspondencia c WHERE c.activo = true AND " +
           "(:texto IS NULL OR LOWER(c.asunto) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(c.numeroInterno) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(c.codigoDocumento) LIKE LOWER(CONCAT('%',:texto,'%'))) AND " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           "(:prioridad IS NULL OR c.prioridad = :prioridad) AND " +
           "(:idTipoDocumento IS NULL OR c.tipoDocumento.idTipoDocumento = :idTipoDocumento) AND " +
           "(:idResponsable IS NULL OR c.responsable.idUsuario = :idResponsable) AND " +
           "(:fechaDesde IS NULL OR c.fechaRecepcion >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR c.fechaRecepcion <= :fechaHasta) " +
           "ORDER BY c.creadoEn DESC")
    Page<Correspondencia> buscar(@Param("texto") String texto,
                                  @Param("estado") String estado,
                                  @Param("prioridad") String prioridad,
                                  @Param("idTipoDocumento") Integer idTipoDocumento,
                                  @Param("idResponsable") Integer idResponsable,
                                  @Param("fechaDesde") LocalDate fechaDesde,
                                  @Param("fechaHasta") LocalDate fechaHasta,
                                  Pageable pageable);

    @Query("SELECT c FROM Correspondencia c WHERE c.activo = true AND c.requiereRespuesta = true AND c.fechaLimiteRespuesta < CURRENT_DATE AND c.estado <> 'ARCHIVADO' AND c.estado <> 'RESPONDIDO'")
    List<Correspondencia> findVencidos();

    @Query("SELECT COUNT(c) FROM Correspondencia c WHERE c.activo = true")
    long countActivos();

    @Query("SELECT c.estado, COUNT(c) FROM Correspondencia c WHERE c.activo = true GROUP BY c.estado")
    List<Object[]> countByEstado();

    @Query("SELECT c.prioridad, COUNT(c) FROM Correspondencia c WHERE c.activo = true GROUP BY c.prioridad")
    List<Object[]> countByPrioridad();

    @Query("SELECT c.tipoDocumento.nombre, COUNT(c) FROM Correspondencia c WHERE c.activo = true GROUP BY c.tipoDocumento.nombre")
    List<Object[]> countByTipoDocumento();

    @Query("SELECT c.departamentoRemitente, COUNT(c) FROM Correspondencia c WHERE c.activo = true AND c.departamentoRemitente IS NOT NULL GROUP BY c.departamentoRemitente")
    List<Object[]> countByDepartamentoRemitente();

    @Query("SELECT FUNCTION('TO_CHAR', c.creadoEn, 'YYYY-MM'), COUNT(c) FROM Correspondencia c WHERE c.activo = true GROUP BY FUNCTION('TO_CHAR', c.creadoEn, 'YYYY-MM') ORDER BY 1")
    List<Object[]> tendenciasMensuales();

    @Query("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (r.creadoEn - c.creadoEn)) / 3600), 0) FROM Correspondencia c JOIN CorrespondenciaRespuesta r ON r.correspondencia = c WHERE c.activo = true AND r.creadoEn IS NOT NULL")
    Double tiempoPromedioRespuestaHoras();

    @Query("SELECT COUNT(c) FROM Correspondencia c WHERE c.activo = true AND c.generaTicket = true")
    long countQueGeneraronTicket();

    @Query("SELECT ct.ticket.idTicket, ct.ticket.numeroTicket, ct.ticket.asunto, ct.ticket.estado FROM CorrespondenciaTicket ct WHERE ct.correspondencia.idCorrespondencia = :idCorrespondencia")
    List<Object[]> findTicketsByCorrespondenciaId(@Param("idCorrespondencia") Integer idCorrespondencia);
}
