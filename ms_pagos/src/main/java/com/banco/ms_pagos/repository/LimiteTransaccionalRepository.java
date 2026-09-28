package com.banco.ms_pagos.repository;

import com.banco.ms_pagos.entity.LimiteTransaccional;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface LimiteTransaccionalRepository extends ReactiveCrudRepository<LimiteTransaccional, Long> {

    Mono<Boolean> existsByClienteIdAndTipoLimiteAndFechaReferencia(
            Long clienteId, String tipoLimite, LocalDate fechaReferencia);

    Mono<LimiteTransaccional> findByClienteIdAndTipoLimiteAndFechaReferencia(
            Long clienteId, String tipoLimite, LocalDate fechaReferencia);
}
