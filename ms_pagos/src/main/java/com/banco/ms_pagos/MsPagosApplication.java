package com.banco.ms_pagos;

import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class MsPagosApplication {

    static {
        Hooks.enableAutomaticContextPropagation();
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                "traceId",
                () -> MDC.get("traceId"),
                value -> MDC.put("traceId", (String) value),
                () -> MDC.remove("traceId")
        );
    }

    public static void main(String[] args) {
        SpringApplication.run(MsPagosApplication.class, args);
    }
}
