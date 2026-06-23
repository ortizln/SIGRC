package com.epmapa.sigrc.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "versiones_tickets", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVersionTicket;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_version", nullable = false)
    private Version version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false)
    private Ticket ticket;
}
