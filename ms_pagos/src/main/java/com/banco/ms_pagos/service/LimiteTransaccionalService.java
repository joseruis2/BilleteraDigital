package com.banco.ms_pagos.service;

import com.banco.ms_pagos.config.LimitesConfig;
import com.banco.ms_pagos.dto.response.LimiteTransaccionalResponse;
import com.banco.ms_pagos.dto.response.MovimientoLimiteResponse;
import com.banco.ms_pagos.entity.LimiteTransaccional;
import com.banco.ms_pagos.event.ClienteRegistradoPayload;
import com.banco.ms_pagos.exception.LimiteExcedidoException;
import com.banco.ms_pagos.repository.LimiteTransaccionalRepository;
import com.banco.ms_pagos.repository.TransferenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.banco.ms_pagos.config.LimitesConfig;
import com.banco.ms_pagos.dto.response.LimiteTransaccionalResponse;
import com.banco.ms_pagos.dto.response.MovimientoLimiteResponse;
import com.banco.ms_pagos.entity.Transferencia;
import com.banco.ms_pagos.repository.TransferenciaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimiteTransaccionalService {

    private static final String TIPO_DIARIO = "DIARIO";

    private final LimiteTransaccionalRepository limiteTransaccionalRepository;
    private final DatabaseClient databaseClient;
    private final TransferenciaRepository transferenciaRepository;

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

    /**
     * Reserva atomicamente el monto en el limite DIARIO de hoy. Como la fila del
     * dia solo se crea una vez (al registrar el cliente), aqui la creamos "al
     * vuelo" si todavia no existe para hoy -- eso es lo que hace que el limite
     * se "reinicie" cada dia sin necesidad de un job programado.
     */
    public Mono<Void> reservarLimite(Long clienteId, BigDecimal monto) {
        LocalDate hoy = LocalDate.now();

        return intentarReservarSobreFilaExistente(clienteId, hoy, monto)
                .flatMap(reservado -> {
                    if (reservado) {
                        return Mono.empty();
                    }
                    // No se pudo reservar: o no existe la fila de hoy, o el limite ya esta copado.
                    return limiteTransaccionalRepository
                            .existsByClienteIdAndTipoLimiteAndFechaReferencia(clienteId, TIPO_DIARIO, hoy)
                            .flatMap(existeHoy -> {
                                if (existeHoy) {
                                    return Mono.error(new LimiteExcedidoException(
                                            "El limite diario de S/. " + LimitesConfig.LIMITE_DIARIO_DEFAULT
                                                    + " ya fue alcanzado. Intenta nuevamente manana."));
                                }
                                return crearFilaDeHoyConReserva(clienteId, hoy, monto);
                            });
                });
    }

    /** Compensacion: si la transferencia falla despues de reservar el limite, se libera. */
    public Mono<Void> liberarLimite(Long clienteId, BigDecimal monto) {
        LocalDate hoy = LocalDate.now();
        return databaseClient.sql("""
                        UPDATE limite_transaccional SET monto_usado = monto_usado - :monto
                        WHERE cliente_id = :clienteId AND tipo_limite = :tipo AND fecha_referencia = :hoy
                        """)
                .bind("monto", monto)
                .bind("clienteId", clienteId)
                .bind("tipo", TIPO_DIARIO)
                .bind("hoy", hoy)
                .fetch()
                .rowsUpdated()
                .doOnNext(filas -> log.info("Limite liberado para clienteId={}, monto={}", clienteId, monto))
                .then();
    }

    private Mono<Boolean> intentarReservarSobreFilaExistente(Long clienteId, LocalDate hoy, BigDecimal monto) {
        return databaseClient.sql("""
                        UPDATE limite_transaccional SET monto_usado = monto_usado + :monto
                        WHERE cliente_id = :clienteId AND tipo_limite = :tipo AND fecha_referencia = :hoy
                          AND monto_usado + :monto <= monto_limite
                        """)
                .bind("monto", monto)
                .bind("clienteId", clienteId)
                .bind("tipo", TIPO_DIARIO)
                .bind("hoy", hoy)
                .fetch()
                .rowsUpdated()
                .map(filas -> filas > 0);
    }

    private Mono<Void> crearFilaDeHoyConReserva(Long clienteId, LocalDate hoy, BigDecimal monto) {
        if (monto.compareTo(LimitesConfig.LIMITE_DIARIO_DEFAULT) > 0) {
            return Mono.error(new LimiteExcedidoException(
                    "El monto supera el limite diario de S/. " + LimitesConfig.LIMITE_DIARIO_DEFAULT));
        }

        LimiteTransaccional limite = LimiteTransaccional.builder()
                .clienteId(clienteId)
                .tipoLimite(TIPO_DIARIO)
                .montoLimite(LimitesConfig.LIMITE_DIARIO_DEFAULT)
                .montoUsado(monto)
                .fechaReferencia(hoy)
                .build();

        return limiteTransaccionalRepository.save(limite)
                .then()
                .onErrorResume(DataIntegrityViolationException.class, e ->
                        // Carrera: otra transferencia creo la fila de hoy justo antes. Reintentamos una vez.
                        intentarReservarSobreFilaExistente(clienteId, hoy, monto)
                                .flatMap(reservado -> reservado
                                        ? Mono.empty()
                                        : Mono.error(new LimiteExcedidoException(
                                        "El limite diario ya fue alcanzado. Intenta nuevamente manana."))));
    }


    public Mono<LimiteTransaccionalResponse> obtenerLimiteDelDia(Long clienteId) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDelDia = hoy.atStartOfDay();
        LocalDateTime finDelDia = hoy.plusDays(1).atStartOfDay();

        Mono<LimiteTransaccional> limiteMono = limiteTransaccionalRepository
                .findByClienteIdAndTipoLimiteAndFechaReferencia(clienteId, TIPO_DIARIO, hoy)
                .defaultIfEmpty(limiteVacioPorDefecto(clienteId, hoy));

        Mono<List<MovimientoLimiteResponse>> historialMono = transferenciaRepository
                .findByClienteOrigenIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
                        clienteId, inicioDelDia, finDelDia)
                .map(this::toMovimientoResponse)
                .collectList();

        return Mono.zip(limiteMono, historialMono)
                .map(tuple -> {
                    LimiteTransaccional limite = tuple.getT1();
                    List<MovimientoLimiteResponse> historial = tuple.getT2();
                    BigDecimal disponible = limite.getMontoLimite().subtract(limite.getMontoUsado());

                    return new LimiteTransaccionalResponse(
                            limite.getMontoLimite(),
                            limite.getMontoUsado(),
                            disponible,
                            historial
                    );
                });
    }

    private LimiteTransaccional limiteVacioPorDefecto(Long clienteId, LocalDate hoy) {
        return LimiteTransaccional.builder()
                .clienteId(clienteId)
                .tipoLimite(TIPO_DIARIO)
                .montoLimite(LimitesConfig.LIMITE_DIARIO_DEFAULT)
                .montoUsado(BigDecimal.ZERO)
                .fechaReferencia(hoy)
                .build();
    }

    private MovimientoLimiteResponse toMovimientoResponse(Transferencia t) {
        return new MovimientoLimiteResponse(
                t.getCodigoUnico(),
                t.getConcepto(),
                t.getMonto(),
                t.getFechaCreacion(),
                t.getEstado()
        );
    }
}
