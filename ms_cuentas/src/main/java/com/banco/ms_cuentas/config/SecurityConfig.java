package com.banco.ms_cuentas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**").permitAll()
                        // Los endpoints internos ya quedan protegidos por
                        // InternalApiKeyFilter (corre antes, con HIGHEST_PRECEDENCE),
                        // asi que aqui se dejan pasar sin exigir JWT.
                        .pathMatchers("/api/cuentas/{id}/debitar",
                                "/api/cuentas/{id}/acreditar",
                                "/api/cuentas/{id}/revertir-debito",
                                "/api/cuentas/buscar",
                                "/api/cuentas/{id}").permitAll()
                        // Cara al cliente: exige JWT valido
                        .pathMatchers("/api/cuentas/mias").authenticated()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
