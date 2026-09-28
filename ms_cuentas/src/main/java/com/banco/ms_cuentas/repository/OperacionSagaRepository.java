package com.banco.ms_cuentas.repository;

import com.banco.ms_cuentas.entity.OperacionSaga;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OperacionSagaRepository extends ReactiveCrudRepository<OperacionSaga, Long> {

    Mono<OperacionSaga> findByReferenciaAndTipoOperacion(String referencia, String tipoOperacion);
}
