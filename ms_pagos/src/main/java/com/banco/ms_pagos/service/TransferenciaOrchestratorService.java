package com.banco.ms_pagos.service;

import com.banco.ms_pagos.client.CuentaClient;
import com.banco.ms_pagos.dto.request.TransferenciaRequest;
import com.banco.ms_pagos.dto.response.CuentaResponse;
import com.banco.ms_pagos.dto.response.HistorialTransferenciaResponse;
import com.banco.ms_pagos.dto.response.TransferenciaResponse;
import com.banco.ms_pagos.entity.Transferencia;
import com.banco.ms_pagos.exception.CuentaNoPerteneceAlClienteException;
import com.banco.ms_pagos.repository.TransferenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferenciaOrchestratorService {

    private final TransferenciaRepository transferenciaRepository;
    private final LimiteTransaccionalService limiteTransaccionalService;
    private final CuentaClient cuentaClient;

    public Mono<TransferenciaResponse> ejecutar(TransferenciaRequest request, Long clienteIdAutenticado) {
        String idempotencyKey = UUID.randomUUID().toString();
        String sagaId = UUID.randomUUID().toString();

        return cuentaClient.obtenerPorId(request.cuentaOrigenId())
                .flatMap(cuentaOrigen -> {
                    if (!cuentaOrigen.clienteId().equals(clienteIdAutenticado)) {
                        return Mono.error(new CuentaNoPerteneceAlClienteException(
                                "La cuenta origen no pertenece al cliente autenticado"));
                    }
                    return cuentaClient.buscarPorNumeroCuenta(request.numeroCuentaDestino())
                            .flatMap(cuentaDestino -> procesar(request, cuentaOrigen, cuentaDestino, idempotencyKey, sagaId));
                });
    }

    private Mono<TransferenciaResponse> procesar(TransferenciaRequest request, CuentaResponse origen,
                                                 CuentaResponse destino, String idempotencyKey, String sagaId) {

        Transferencia transferencia = Transferencia.builder()
                .codigoUnico(generarCodigoUnico())
                .idempotencyKey(idempotencyKey)
                .sagaId(sagaId)
                .cuentaOrigenId(origen.id())
                .cuentaDestinoId(destino.id())
                .clienteOrigenId(origen.clienteId())
                .monto(request.monto())
                .concepto(request.concepto())
                .estado("INICIADA")
                .pasoSaga("VALIDANDO_LIMITE")
                .build();

        return transferenciaRepository.save(transferencia)
                .flatMap(t -> limiteTransaccionalService.reservarLimite(origen.clienteId(), request.monto())
                        .then(Mono.defer(() -> avanzarADebito(t)))
                        .onErrorResume(error -> marcarFallidaSinCompensacionPrevia(t, error)));
    }

    private Mono<TransferenciaResponse> avanzarADebito(Transferencia t) {
        t.setPasoSaga("DEBITANDO_ORIGEN");
        return transferenciaRepository.save(t)
                .flatMap(saved -> cuentaClient.debitar(saved.getCuentaOrigenId(), saved.getIdempotencyKey(), saved.getMonto())
                        .then(Mono.defer(() -> avanzarACredito(saved)))
                        .onErrorResume(error -> compensarLimiteYFallar(saved, error)));
    }

    private Mono<TransferenciaResponse> avanzarACredito(Transferencia t) {
        t.setPasoSaga("ACREDITANDO_DESTINO");
        return transferenciaRepository.save(t)
                .flatMap(saved -> cuentaClient.acreditar(saved.getCuentaDestinoId(), saved.getIdempotencyKey(), saved.getMonto())
                        .then(Mono.defer(() -> completar(saved)))
                        .onErrorResume(error -> compensarDebitoYFallar(saved, error)));
    }

    private Mono<TransferenciaResponse> completar(Transferencia t) {
        t.setEstado("COMPLETADA");
        t.setPasoSaga("FINALIZADA");
        return transferenciaRepository.save(t)
                .doOnSuccess(saved -> log.info("Transferencia {} completada", saved.getCodigoUnico()))
                .map(this::toResponse);
    }

    /** El credito al destino fallo: hay que devolver el dinero al origen y liberar el limite. */
    private Mono<TransferenciaResponse> compensarDebitoYFallar(Transferencia t, Throwable error) {
        log.warn("Credito fallo para transferencia {}, compensando debito. Motivo: {}",
                t.getCodigoUnico(), error.getMessage());

        t.setPasoSaga("COMPENSANDO");
        return transferenciaRepository.save(t)
                .flatMap(saved -> cuentaClient
                        .revertirDebito(saved.getCuentaOrigenId(), saved.getIdempotencyKey() + ":REVERSION", saved.getMonto())
                        .then(Mono.defer(() -> compensarLimiteYFallar(saved, error))));
    }

    /** El debito al origen fallo (o la compensacion de arriba ya devolvio el dinero): solo falta liberar el limite. */
    private Mono<TransferenciaResponse> compensarLimiteYFallar(Transferencia t, Throwable error) {
        return limiteTransaccionalService.liberarLimite(t.getCuentaOrigenId(), t.getMonto())
                .then(Mono.defer(() -> marcarFallidaSinCompensacionPrevia(t, error)));
    }

    private Mono<TransferenciaResponse> marcarFallidaSinCompensacionPrevia(Transferencia t, Throwable error) {
        t.setEstado("FALLIDA");
        t.setPasoSaga("FINALIZADA");
        t.setMotivoFallo(error.getMessage());
        return transferenciaRepository.save(t)
                .doOnSuccess(saved -> log.warn("Transferencia {} marcada como FALLIDA: {}",
                        saved.getCodigoUnico(), saved.getMotivoFallo()))
                .map(this::toResponse);
    }

    private TransferenciaResponse toResponse(Transferencia t) {
        return new TransferenciaResponse(t.getCodigoUnico(), t.getEstado(), t.getMonto(), t.getConcepto(),
                t.getCuentaOrigenId(), t.getCuentaDestinoId(), t.getFechaModificacion(), t.getMotivoFallo());
    }

    private String generarCodigoUnico() {
        return "TRF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }


    public Flux<HistorialTransferenciaResponse> obtenerHistorial(Long clienteId) {
        return transferenciaRepository.findByClienteOrigenIdOrderByFechaCreacionDesc(clienteId)
                .map(this::toHistorialResponse);
    }

    private HistorialTransferenciaResponse toHistorialResponse(Transferencia t) {
        return new HistorialTransferenciaResponse(
                t.getCodigoUnico(),
                t.getEstado(),
                t.getMonto(),
                t.getConcepto(),
                t.getCuentaDestinoId(),
                t.getFechaCreacion(),
                t.getMotivoFallo()
        );
    }
}
