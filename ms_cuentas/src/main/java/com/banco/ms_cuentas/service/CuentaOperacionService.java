package com.banco.ms_cuentas.service;


import com.banco.ms_cuentas.dto.request.OperacionCuentaRequest;
import com.banco.ms_cuentas.dto.response.OperacionCuentaResponse;
import com.banco.ms_cuentas.entity.OperacionSaga;
import com.banco.ms_cuentas.exception.CuentaNoEncontradaException;
import com.banco.ms_cuentas.exception.SaldoInsuficienteException;
import com.banco.ms_cuentas.repository.OperacionSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaOperacionService {

    private static final String TIPO_DEBITO = "DEBITO";
    private static final String TIPO_CREDITO = "CREDITO";
    private static final String TIPO_REVERSION_DEBITO = "REVERSION_DEBITO";

    private final DatabaseClient databaseClient;
    private final OperacionSagaRepository operacionSagaRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<OperacionCuentaResponse> debitar(Long cuentaId, OperacionCuentaRequest request) {
        return ejecutarConIdempotencia(cuentaId, request, TIPO_DEBITO, this::aplicarDebito);
    }

    public Mono<OperacionCuentaResponse> acreditar(Long cuentaId, OperacionCuentaRequest request) {
        return ejecutarConIdempotencia(cuentaId, request, TIPO_CREDITO, this::aplicarCredito);
    }

    /**
     * Usada por el orquestador para revertir un debito cuando el credito al
     * destino falla. Tecnicamente es un credito, pero se guarda con su propio
     * tipo de operacion para que quede clara la intencion en la auditoria.
     */
    public Mono<OperacionCuentaResponse> revertirDebito(Long cuentaId, OperacionCuentaRequest request) {
        return ejecutarConIdempotencia(cuentaId, request, TIPO_REVERSION_DEBITO, this::aplicarCredito);
    }

    private Mono<OperacionCuentaResponse> ejecutarConIdempotencia(
            Long cuentaId,
            OperacionCuentaRequest request,
            String tipoOperacion,
            OperacionAtomica operacion) {

        return operacionSagaRepository.findByReferenciaAndTipoOperacion(request.referencia(), tipoOperacion)
                .map(existente -> {
                    log.info("Operacion {} con referencia={} ya fue procesada antes, devolviendo resultado guardado",
                            tipoOperacion, request.referencia());
                    return new OperacionCuentaResponse(existente.getCuentaId(), existente.getSaldoResultante());
                })
                .switchIfEmpty(Mono.defer(() ->
                        operacion.aplicar(cuentaId, request.monto())
                                .flatMap(saldoResultante -> guardarOperacionSaga(
                                        cuentaId, request, tipoOperacion, saldoResultante))
                                .as(transactionalOperator::transactional)
                                .onErrorResume(DataIntegrityViolationException.class, e ->
                                        // Carrera rarisima: dos llamadas concurrentes con la misma referencia.
                                        // La que perdio la carrera del INSERT unique, relee el resultado de la ganadora.
                                        operacionSagaRepository
                                                .findByReferenciaAndTipoOperacion(request.referencia(), tipoOperacion)
                                                .map(existente -> new OperacionCuentaResponse(
                                                        existente.getCuentaId(), existente.getSaldoResultante())))
                ));
    }

    private Mono<OperacionCuentaResponse> guardarOperacionSaga(
            Long cuentaId, OperacionCuentaRequest request, String tipoOperacion, BigDecimal saldoResultante) {

        OperacionSaga registro = OperacionSaga.builder()
                .referencia(request.referencia())
                .tipoOperacion(tipoOperacion)
                .cuentaId(cuentaId)
                .monto(request.monto())
                .saldoResultante(saldoResultante)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return operacionSagaRepository.save(registro)
                .thenReturn(new OperacionCuentaResponse(cuentaId, saldoResultante));
    }

    @FunctionalInterface
    private interface OperacionAtomica {
        Mono<BigDecimal> aplicar(Long cuentaId, BigDecimal monto);
    }

    private Mono<BigDecimal> aplicarDebito(Long cuentaId, BigDecimal monto) {
        // La condicion "AND saldo >= :monto" en el propio UPDATE es lo que hace
        // esto atomico: si dos requests llegan a la vez, la base de datos serializa
        // los updates y solo uno puede dejar el saldo en positivo.
        return databaseClient.sql("""
                        UPDATE cuenta SET saldo = saldo - :monto
                        WHERE id = :id AND activo = TRUE AND saldo >= :monto
                        """)
                .bind("monto", monto)
                .bind("id", cuentaId)
                .fetch()
                .rowsUpdated()
                .flatMap(filasActualizadas -> {
                    if (filasActualizadas == 0) {
                        return Mono.error(new SaldoInsuficienteException(
                                "Saldo insuficiente o cuenta inactiva para la cuenta " + cuentaId));
                    }
                    return obtenerSaldoActual(cuentaId);
                });
    }

    private Mono<BigDecimal> aplicarCredito(Long cuentaId, BigDecimal monto) {
        return databaseClient.sql("""
                        UPDATE cuenta SET saldo = saldo + :monto
                        WHERE id = :id AND activo = TRUE
                        """)
                .bind("monto", monto)
                .bind("id", cuentaId)
                .fetch()
                .rowsUpdated()
                .flatMap(filasActualizadas -> {
                    if (filasActualizadas == 0) {
                        return Mono.error(new CuentaNoEncontradaException(
                                "Cuenta " + cuentaId + " no encontrada o inactiva"));
                    }
                    return obtenerSaldoActual(cuentaId);
                });
    }

    private Mono<BigDecimal> obtenerSaldoActual(Long cuentaId) {
        return databaseClient.sql("SELECT saldo FROM cuenta WHERE id = :id")
                .bind("id", cuentaId)
                .map(row -> row.get("saldo", BigDecimal.class))
                .one();
    }
}
