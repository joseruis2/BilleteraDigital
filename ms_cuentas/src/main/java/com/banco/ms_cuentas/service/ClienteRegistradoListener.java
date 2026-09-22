package com.banco.ms_cuentas.service;

import com.banco.ms_cuentas.event.ClienteRegistradoPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteRegistradoListener {

    private final CuentaService cuentaService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "cliente-registrado", groupId = "ms-cuentas-group")
    public void escuchar(String mensaje) {
        ClienteRegistradoPayload payload;
        try {
            payload = objectMapper.readValue(mensaje, ClienteRegistradoPayload.class);
        } catch (Exception e) {
            log.error("No se pudo deserializar el mensaje del topico cliente-registrado: {}", mensaje, e);
            return;
        }

        log.info("Evento CLIENTE_REGISTRADO recibido en ms-cuentas: clienteId={}", payload.clienteId());

        // Bloqueamos a proposito: el offset de Kafka solo se confirma si el insert tuvo exito.
        cuentaService.crearCuentaAhorroDesdeEvento(payload).block();
    }
}
