package com.banco.ms_cliente.service;


import com.banco.ms_cliente.config.KafkaTopics;
import com.banco.ms_cliente.entity.OutboxEvent;
import com.banco.ms_cliente.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Relay del transactional outbox: cada 3s revisa outbox_event con publicado=false,
 * los publica a Kafka y marca publicado=true. Es "at-least-once": si falla entre
 * el envío a Kafka y el update en BD, el evento se reenvía en el siguiente ciclo,
 * por eso el consumidor (ms-auth, ms-cuentas) debe ser idempotente.
 *
 * Nota: para producción real, considera migrar esto a Debezium (CDC sobre el binlog)
 * en vez de polling, para no golpear la tabla constantemente y tener menor latencia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    public void publicarEventosPendientes() {
        outboxEventRepository.findByPublicadoFalseOrderByFechaCreacionAsc()
                .flatMap(this::publicarYMarcar)
                .doOnError(e -> log.error("Error en el ciclo del outbox relay", e))
                .onErrorResume(e -> Mono.empty()) // no tumbar el scheduler por un fallo puntual
                .subscribe();
    }

    private Mono<OutboxEvent> publicarYMarcar(OutboxEvent event) {
        String topic = resolverTopico(event.getTipoEvento());

        return Mono.fromFuture(kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                        .toCompletableFuture())
                .doOnSuccess(result -> log.info("Evento {} (id={}) publicado en topico {}",
                        event.getTipoEvento(), event.getId(), topic))
                .then(marcarComoPublicado(event))
                .doOnError(e -> log.error("Fallo publicando evento id={} en Kafka, se reintenta en el siguiente ciclo",
                        event.getId(), e));
    }

    private Mono<OutboxEvent> marcarComoPublicado(OutboxEvent event) {
        event.setPublicado(true);
        event.setFechaPublicacion(LocalDateTime.now());
        return outboxEventRepository.save(event);
    }

    private String resolverTopico(String tipoEvento) {
        return switch (tipoEvento) {
            case "CLIENTE_REGISTRADO" -> KafkaTopics.CLIENTE_REGISTRADO;
            default -> throw new IllegalArgumentException("Tipo de evento sin topico mapeado: " + tipoEvento);
        };
    }
}
