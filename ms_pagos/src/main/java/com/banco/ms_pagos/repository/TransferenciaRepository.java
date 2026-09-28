package com.banco.ms_pagos.repository;

import com.banco.ms_pagos.entity.Transferencia;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface TransferenciaRepository extends ReactiveCrudRepository<Transferencia, Long> {

    Mono<Transferencia> findByIdempotencyKey(String idempotencyKey);

    Flux<Transferencia> findByClienteOrigenIdOrderByFechaCreacionDesc(Long clienteOrigenId);

    Flux<Transferencia> findByClienteOrigenIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            Long clienteOrigenId, LocalDateTime desde, LocalDateTime hasta);
}