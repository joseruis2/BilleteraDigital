package com.banco.ms_cuentas.repository;

import com.banco.ms_cuentas.entity.Cuenta;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CuentaRepository extends ReactiveCrudRepository<Cuenta, Long> {

    // Idempotencia: si el evento se reprocesa, no duplicamos la cuenta
    Mono<Boolean> existsByClienteIdAndProductoId(Long clienteId, Long productoId);

    // Resuelve el "numero de cuenta" que se usa para buscar al destinatario de una transferencia
    Mono<Cuenta> findByCodigoUnico(String codigoUnico);

    Flux<Cuenta> findByClienteId(Long clienteId);
}
