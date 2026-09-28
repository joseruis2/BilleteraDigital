package com.banco.ms_pagos.client;


import com.banco.ms_pagos.dto.request.OperacionCuentaRequest;
import com.banco.ms_pagos.dto.response.CuentaResponse;
import com.banco.ms_pagos.dto.response.OperacionCuentaResponse;
import com.banco.ms_pagos.exception.CuentaNoEncontradaException;
import com.banco.ms_pagos.exception.SaldoInsuficienteException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CuentaClient {

    private final WebClient cuentasWebClient;

    public Mono<CuentaResponse> buscarPorNumeroCuenta(String numeroCuenta) {
        return cuentasWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/cuentas/buscar")
                        .queryParam("numeroCuenta", numeroCuenta)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapearError404)
                .bodyToMono(CuentaResponse.class);
    }

    public Mono<CuentaResponse> obtenerPorId(Long cuentaId) {
        return cuentasWebClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapearError404)
                .bodyToMono(CuentaResponse.class);
    }

    public Mono<OperacionCuentaResponse> debitar(Long cuentaId, String referencia, java.math.BigDecimal monto) {
        return cuentasWebClient.post()
                .uri("/api/cuentas/{id}/debitar", cuentaId)
                .bodyValue(new OperacionCuentaRequest(monto, referencia))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapearErrorDebito)
                .bodyToMono(OperacionCuentaResponse.class);
    }

    public Mono<OperacionCuentaResponse> acreditar(Long cuentaId, String referencia, java.math.BigDecimal monto) {
        return cuentasWebClient.post()
                .uri("/api/cuentas/{id}/acreditar", cuentaId)
                .bodyValue(new OperacionCuentaRequest(monto, referencia))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapearError404)
                .bodyToMono(OperacionCuentaResponse.class);
    }

    public Mono<OperacionCuentaResponse> revertirDebito(Long cuentaId, String referencia, java.math.BigDecimal monto) {
        return cuentasWebClient.post()
                .uri("/api/cuentas/{id}/revertir-debito", cuentaId)
                .bodyValue(new OperacionCuentaRequest(monto, referencia))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapearError404)
                .bodyToMono(OperacionCuentaResponse.class);
    }

    private Mono<? extends Throwable> mapearError404(org.springframework.web.reactive.function.client.ClientResponse response) {
        return Mono.error(new CuentaNoEncontradaException("Cuenta no encontrada en ms-cuentas"));
    }

    private Mono<? extends Throwable> mapearErrorDebito(org.springframework.web.reactive.function.client.ClientResponse response) {
        if (response.statusCode().value() == 422) {
            return Mono.error(new SaldoInsuficienteException("Saldo insuficiente en la cuenta origen"));
        }
        return Mono.error(new CuentaNoEncontradaException("Error al debitar la cuenta en ms-cuentas"));
    }
}
