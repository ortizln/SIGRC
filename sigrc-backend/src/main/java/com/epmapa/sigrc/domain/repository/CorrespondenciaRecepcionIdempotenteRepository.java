package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.CorrespondenciaRecepcionIdempotente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorrespondenciaRecepcionIdempotenteRepository
        extends JpaRepository<CorrespondenciaRecepcionIdempotente, Integer> {

    Optional<CorrespondenciaRecepcionIdempotente> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
