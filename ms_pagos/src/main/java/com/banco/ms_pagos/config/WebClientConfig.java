package com.banco.ms_pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // Direccion fija por ahora (localhost:8083). Cuando Eureka este estable,
    // esto se reemplaza por un WebClient @LoadBalanced apuntando a "ms-cuentas"
    // por nombre de servicio en vez de host:puerto.
    @Bean
    public WebClient cuentasWebClient(@Value("${ms-cuentas.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
