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

    @Query(value = "SELECT c.id_correspondencia, c.activo, CAST(c.asunto AS TEXT) as asunto, c.cargo, CAST(c.codigo_documento AS TEXT) as codigo_documento, c.creado_en, c.creado_por, c.departamento_remitente, c.estado, c.fecha_documento, c.fecha_limite_respuesta, c.fecha_recepcion, c.genera_ticket, c.hora_recepcion, c.institucion, CAST(c.numero_interno AS TEXT) as numero_interno, c.observaciones, c.persona_entrega, c.prioridad, c.requiere_respuesta, c.resumen_ejecutivo, c.sentido, c.id_responsable, c.id_tipo_documento FROM sigrc.correspondencia c WHERE c.activo = true AND " +
           "(:texto IS NULL OR LOWER(CAST(c.asunto AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(CAST(c.numero_interno AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(CAST(c.codigo_documento AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%'))) AND " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           "(:prioridad IS NULL OR c.prioridad = :prioridad) AND " +
           "(:idTipoDocumento IS NULL OR c.id_tipo_documento = :idTipoDocumento) AND " +
            "((:idResponsable IS NULL AND :idUsuario IS NULL) OR EXISTS (SELECT 1 FROM sigrc.correspondencia_responsable cr WHERE cr.id_correspondencia = c.id_correspondencia AND cr.id_usuario = :idResponsable) OR c.creado_por = :idUsuario) AND " +
            "(:sentido IS NULL OR c.sentido = :sentido) AND " +
             "(CAST(:fechaDesde AS date) IS NULL OR c.fecha_recepcion >= CAST(:fechaDesde AS date)) AND " +
             "(CAST(:fechaHasta AS date) IS NULL OR c.fecha_recepcion <= CAST(:fechaHasta AS date))",
             countQuery = "SELECT COUNT(*) FROM sigrc.correspondencia c WHERE c.activo = true AND " +
            "(:texto IS NULL OR LOWER(CAST(c.asunto AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(CAST(c.numero_interno AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(CAST(c.codigo_documento AS TEXT)) LIKE LOWER(CONCAT('%',:texto,'%'))) AND " +
            "(:estado IS NULL OR c.estado = :estado) AND " +
            "(:prioridad IS NULL OR c.prioridad = :prioridad) AND " +
            "(:idTipoDocumento IS NULL OR c.id_tipo_documento = :idTipoDocumento) AND " +
            "((:idResponsable IS NULL AND :idUsuario IS NULL) OR EXISTS (SELECT 1 FROM sigrc.correspondencia_responsable cr WHERE cr.id_correspondencia = c.id_correspondencia AND cr.id_usuario = :idResponsable) OR c.creado_por = :idUsuario) AND " +
            "(:sentido IS NULL OR c.sentido = :sentido) AND " +
             "(CAST(:fechaDesde AS date) IS NULL OR c.fecha_recepcion >= CAST(:fechaDesde AS date)) AND " +
             "(CAST(:fechaHasta AS date) IS NULL OR c.fecha_recepcion <= CAST(:fechaHasta AS date))",
           nativeQuery = true)
    Page<Correspondencia> buscar(@Param("texto") String texto,
                                   @Param("estado") String estado,
                                   @Param("prioridad") String prioridad,
                                   @Param("idTipoDocumento") Integer idTipoDocumento,
                                   @Param("idResponsable") Integer idResponsable,
                                   @Param("idUsuario") Integer idUsuario,
                                   @Param("sentido") String sentido,
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

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (r.creado_en - c.creado_en)) / 3600), 0) FROM sigrc.correspondencia c JOIN sigrc.correspondencia_respuesta r ON r.id_correspondencia = c.id_correspondencia WHERE c.activo = true AND r.creado_en IS NOT NULL", nativeQuery = true)
    Double tiempoPromedioRespuestaHoras();

    @Query("SELECT COUNT(c) FROM Correspondencia c WHERE c.activo = true AND c.generaTicket = true")
    long countQueGeneraronTicket();

    @Query("SELECT ct.ticket.idTicket, ct.ticket.numeroTicket, ct.ticket.asunto, ct.ticket.estado FROM CorrespondenciaTicket ct WHERE ct.correspondencia.idCorrespondencia = :idCorrespondencia")
    List<Object[]> findTicketsByCorrespondenciaId(@Param("idCorrespondencia") Integer idCorrespondencia);

    @Query(value = "SELECT COALESCE(MAX(SPLIT_PART(numero_interno, '-', 3)::INTEGER), 0) FROM sigrc.correspondencia WHERE numero_interno LIKE 'COR-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-%'", nativeQuery = true)
    Integer maxCorrelativoAnioActual();
}
