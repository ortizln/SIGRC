package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaLecturaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CorrespondenciaLecturaUsuarioRepository extends JpaRepository<CorrespondenciaLecturaUsuario, Integer> {

    Optional<CorrespondenciaLecturaUsuario> findByCorrespondenciaIdCorrespondenciaAndUsuarioId(
            Integer idCorrespondencia, Integer usuarioId);

    boolean existsByCorrespondenciaIdCorrespondenciaAndUsuarioId(
            Integer idCorrespondencia, Integer usuarioId);

    List<CorrespondenciaLecturaUsuario> findByCorrespondenciaIdCorrespondenciaInAndUsuarioId(
            List<Integer> idsCorrespondencia, Integer usuarioId);

    @Modifying
    @Query("UPDATE CorrespondenciaLecturaUsuario l SET l.leidoEn = CURRENT_TIMESTAMP " +
           "WHERE l.correspondencia.idCorrespondencia = :idCorrespondencia AND l.usuarioId = :usuarioId")
    void actualizarLectura(@Param("idCorrespondencia") Integer idCorrespondencia,
                           @Param("usuarioId") Integer usuarioId);
}
