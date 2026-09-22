package com.banco.ms_pagos.service;

import com.banco.ms_pagos.config.LimitesConfig;
import com.banco.ms_pagos.entity.LimiteTransaccional;
import com.banco.ms_pagos.event.ClienteRegistradoPayload;
import com.banco.ms_pagos.repository.LimiteTransaccionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimiteTransaccionalService {

    private static final String TIPO_DIARIO = "DIARIO";

    private final LimiteTransaccionalRepository limiteTransaccionalRepository;

    public Mono<LimiteTransaccional> crearLimiteDiarioDesdeEvento(ClienteRegistradoPayload payload) {
        LocalDate hoy = LocalDate.now();

        return limiteTransaccionalRepository
                .existsByClienteIdAndTipoLimiteAndFechaReferencia(payload.clienteId(), TIPO_DIARIO, hoy)
                .flatMap(existe -> {
                    if (existe) {
                        log.info("Limite diario ya existe para clienteId={} en fecha={}, se ignora el evento duplicado",
                                payload.clienteId(), hoy);
                        return Mono.empty();
                    }
                    return crear(payload.clienteId(), hoy);
                });
    }

    private Mono<LimiteTransaccional> crear(Long clienteId, LocalDate fechaReferencia) {
        LimiteTransaccional limite = LimiteTransaccional.builder()
                .clienteId(clienteId)
                .tipoLimite(TIPO_DIARIO)
                .montoLimite(LimitesConfig.LIMITE_DIARIO_DEFAULT)
                .montoUsado(BigDecimal.ZERO)
                .fechaReferencia(fechaReferencia)
                .build();

        return limiteTransaccionalRepository.save(limite)
                .doOnSuccess(l -> log.info("Limite diario creado para clienteId={}, fecha={}, monto={}",
                        l.getClienteId(), l.getFechaReferencia(), l.getMontoLimite()));
    }
}
