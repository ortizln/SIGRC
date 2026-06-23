package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.TicketAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAdjuntoRepository extends JpaRepository<TicketAdjunto, Integer> {
    List<TicketAdjunto> findByTicketIdTicket(Integer idTicket);
}
