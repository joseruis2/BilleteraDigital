package com.banco.ms_auth.service;

import com.banco.ms_auth.event.ClienteRegistradoPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteRegistradoListener {

    private final UsuarioAuthService usuarioAuthService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "cliente-registrado", groupId = "ms-auth-group")
    public void escuchar(String mensaje) {
        ClienteRegistradoPayload payload;
        try {
            payload = objectMapper.readValue(mensaje, ClienteRegistradoPayload.class);
        } catch (Exception e) {
            log.error("No se pudo deserializar el mensaje del topico cliente-registrado: {}", mensaje, e);
            return; // mensaje corrupto: no tiene sentido reintentar, se descarta
        }

        log.info("Evento CLIENTE_REGISTRADO recibido: clienteId={}, username={}",
                payload.clienteId(), payload.username());

        // OJO: bloqueamos aqui a proposito. El hilo del listener de Kafka NO es un
        // event-loop reactivo (es un hilo normal del consumer), asi que bloquear es seguro,
        // y ademas asegura que el offset solo se confirme si el insert en BD tuvo exito.
        // Si esto lanza excepcion, Spring Kafka reintenta el mensaje en el siguiente poll.
        usuarioAuthService.crearUsuarioDesdeEvento(payload).block();
    }
}
