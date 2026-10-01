package com.banco.ms_pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    private static final String HEADER_NAME = "X-Internal-Api-Key";

    // Este builder queda anotado @LoadBalanced: cualquier WebClient armado
    // con el, usando un host como "ms-cuentas" o "ms-cliente" en vez de
    // localhost:puerto, se resuelve automaticamente contra Eureka.
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient cuentasWebClient(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder,
                                      @Value("${internal.api-key}") String internalApiKey) {
        return builder
                .baseUrl("lb://ms-cuentas")
                .filter(agregarApiKeyInterna(internalApiKey))
                .build();
    }

    @Bean
    public WebClient clienteWebClient(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder,
                                      @Value("${internal.api-key}") String internalApiKey) {
        return builder
                .baseUrl("lb://ms-cliente")
                .filter(agregarApiKeyInterna(internalApiKey))
                .build();
    }

    private ExchangeFilterFunction agregarApiKeyInterna(String apiKey) {
        return ExchangeFilterFunction.ofRequestProcessor(request ->
                Mono.just(
                        ClientRequest.from(request)
                                .header(HEADER_NAME, apiKey)
                                .build()));
    }
}
