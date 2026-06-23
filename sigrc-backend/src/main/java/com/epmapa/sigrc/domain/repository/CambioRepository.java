package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Cambio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioRepository extends JpaRepository<Cambio, Integer> {
    List<Cambio> findByEstado(String estado);
    List<Cambio> findBySistemaIdSistema(Integer idSistema);
    List<Cambio> findByTicketIdTicket(Integer idTicket);
}
