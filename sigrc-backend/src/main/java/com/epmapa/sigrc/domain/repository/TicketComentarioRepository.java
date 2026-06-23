package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.TicketComentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketComentarioRepository extends JpaRepository<TicketComentario, Integer> {
    List<TicketComentario> findByTicket_IdTicketOrderByCreadoEnDesc(Integer idTicket);
}
