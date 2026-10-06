package com.banco.ms_cuentas.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Propaga (o genera, si falta) un X-Trace-Id por peticion, para poder
 * rastrear una misma operacion a traves de los logs de varios microservicios.
 *
 * Ojo: va con Order HIGHEST_PRECEDENCE + 1 para correr DESPUES de
 * InternalApiKeyFilter (que usa HIGHEST_PRECEDENCE puro), asi el trace id
 * no se agrega a peticiones que de entrada van a ser rechazadas por auth.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter implements WebFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_CONTEXT_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        String finalTraceId = traceId;
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(TRACE_ID_CONTEXT_KEY, finalTraceId));
    }
}
