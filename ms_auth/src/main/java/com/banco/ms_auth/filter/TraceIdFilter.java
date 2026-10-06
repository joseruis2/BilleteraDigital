package com.banco.ms_auth.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Genera (o propaga) un X-Trace-Id por cada peticion, para poder rastrear
 * una misma operacion a traves de los logs de varios microservicios.
 *
 * Si el header ya viene seteado (por ejemplo, porque lo puso el Gateway o
 * ms-pagos al llamar a ms-cuentas), se reutiliza el mismo valor en vez de
 * generar uno nuevo -- eso es lo que permite correlacionar logs entre
 * servicios distintos para una misma transaccion de negocio.
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
