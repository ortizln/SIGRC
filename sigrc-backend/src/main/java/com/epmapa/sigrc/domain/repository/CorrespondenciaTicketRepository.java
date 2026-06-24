package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrespondenciaTicketRepository extends JpaRepository<CorrespondenciaTicket, Integer> {
    List<CorrespondenciaTicket> findByCorrespondenciaIdCorrespondencia(Integer idCorrespondencia);
    List<CorrespondenciaTicket> findByTicketIdTicket(Integer idTicket);
    boolean existsByCorrespondenciaIdCorrespondenciaAndTicketIdTicket(Integer idCorrespondencia, Integer idTicket);
}
