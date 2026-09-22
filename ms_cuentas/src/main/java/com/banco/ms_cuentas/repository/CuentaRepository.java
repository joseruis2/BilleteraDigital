package com.banco.ms_cuentas.repository;

import com.banco.ms_cuentas.entity.Cuenta;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CuentaRepository extends ReactiveCrudRepository<Cuenta, Long> {

    // Idempotencia: si el evento se reprocesa, no duplicamos la cuenta
    Mono<Boolean> existsByClienteIdAndProductoId(Long clienteId, Long productoId);
}
