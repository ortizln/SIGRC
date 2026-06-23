package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.TicketHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistorialRepository extends JpaRepository<TicketHistorial, Integer> {
    List<TicketHistorial> findByTicketIdTicketOrderByCreadoEnDesc(Integer idTicket);
}
