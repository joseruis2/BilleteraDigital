package com.banco.ms_cliente.repository;



import com.banco.ms_cliente.entity.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    Flux<OutboxEvent> findByPublicadoFalseOrderByFechaCreacionAsc();
}
