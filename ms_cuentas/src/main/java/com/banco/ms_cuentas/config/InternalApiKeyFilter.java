package com.banco.ms_cuentas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * Protege los endpoints INTERNOS de /api/cuentas/** (llamados solo por
 * ms-pagos) con una api key compartida. Los endpoints CARA AL CLIENTE
 * (/mias y /{id}/saldo) quedan excluidos aqui y se protegen en su lugar
 * con JWT de usuario, via Spring Security (ver SecurityConfig).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalApiKeyFilter implements WebFilter {

    private static final String HEADER_NAME = "X-Internal-Api-Key";

    // Endpoints cara al cliente: se protegen con JWT, no con api key.
    private static final Pattern RUTA_MIAS = Pattern.compile("^/api/cuentas/mias$");
    private static final Pattern RUTA_SALDO = Pattern.compile("^/api/cuentas/\\d+/saldo$");

    @Value("${internal.api-key}")
    private String apiKeyEsperada;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        boolean esRutaCaraAlCliente = RUTA_MIAS.matcher(path).matches()
                || RUTA_SALDO.matcher(path).matches();

        if (!path.startsWith("/api/cuentas") || esRutaCaraAlCliente) {
            return chain.filter(exchange);
        }

        String apiKeyRecibida = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);

        if (apiKeyRecibida == null || !coincideEnTiempoConstante(apiKeyRecibida, apiKeyEsperada)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean coincideEnTiempoConstante(String recibida, String esperada) {
        return MessageDigest.isEqual(
                recibida.getBytes(StandardCharsets.UTF_8),
                esperada.getBytes(StandardCharsets.UTF_8)
        );
    }
}