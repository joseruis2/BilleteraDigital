package com.banco.ms_pagos.client;

import com.banco.ms_pagos.dto.response.ClienteInternoResponse;
import com.banco.ms_pagos.exception.CuentaNoEncontradaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ClienteClient {

    private final WebClient clienteWebClient;

    public ClienteClient(@Qualifier("clienteWebClient") WebClient clienteWebClient) {
        this.clienteWebClient = clienteWebClient;
    }

    public Mono<ClienteInternoResponse> obtenerPorId(Long clienteId) {
        return clienteWebClient.get()
                .uri("/api/clientes/{id}", clienteId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapearError404)
                .bodyToMono(ClienteInternoResponse.class);
    }

    private Mono<? extends Throwable> mapearError404(ClientResponse response) {
        return Mono.error(new CuentaNoEncontradaException("Cliente no encontrado en ms-cliente"));
    }
}