package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByDestinatarioIdUsuarioAndLeidoFalseOrderByCreadoEnDesc(Integer idUsuario);
    long countByDestinatarioIdUsuarioAndLeidoFalse(Integer idUsuario);
}
