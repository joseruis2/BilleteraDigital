package com.banco.ms_cuentas.service;

import com.banco.ms_cuentas.dto.request.OperacionCuentaRequest;
import com.banco.ms_cuentas.entity.OperacionSaga;
import com.banco.ms_cuentas.exception.SaldoInsuficienteException;
import com.banco.ms_cuentas.repository.OperacionSagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaOperacionServiceTest {

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private OperacionSagaRepository operacionSagaRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private FetchSpec<java.util.Map<String, Object>> fetchSpec;

    private CuentaOperacionService cuentaOperacionService;

    @BeforeEach
    void setUp() {
        cuentaOperacionService = new CuentaOperacionService(
                databaseClient, operacionSagaRepository, transactionalOperator);

        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void debitar_conSaldoSuficiente_descuentaYGuardaIdempotencia() {
        OperacionCuentaRequest request = new OperacionCuentaRequest(
                new BigDecimal("50.00"), "ref-debito-001");

        when(operacionSagaRepository.findByReferenciaAndTipoOperacion("ref-debito-001", "DEBITO"))
                .thenReturn(Mono.empty());

        GenericExecuteSpec updateDebito = mock(GenericExecuteSpec.class);
        when(databaseClient.sql(contains("UPDATE cuenta SET saldo = saldo - :monto"))).thenReturn(updateDebito);
        when(updateDebito.bind(anyString(), any())).thenReturn(updateDebito);
        when(updateDebito.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        GenericExecuteSpec selectSaldo = mock(GenericExecuteSpec.class);
        RowsFetchSpec<BigDecimal> mapSpec = mock(RowsFetchSpec.class);
        when(databaseClient.sql(contains("SELECT saldo FROM cuenta"))).thenReturn(selectSaldo);
        when(selectSaldo.bind(anyString(), any())).thenReturn(selectSaldo);
        when(selectSaldo.map(any(Function.class))).thenReturn(mapSpec);
        when(mapSpec.one()).thenReturn(Mono.just(new BigDecimal("450.00")));

        when(operacionSagaRepository.save(any(OperacionSaga.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cuentaOperacionService.debitar(3L, request))
                .expectNextMatches(response ->
                        response.cuentaId().equals(3L)
                                && response.saldoResultante().compareTo(new BigDecimal("450.00")) == 0)
                .verifyComplete();

        verify(operacionSagaRepository).save(argThat(saga ->
                "DEBITO".equals(saga.getTipoOperacion())
                        && "ref-debito-001".equals(saga.getReferencia())
                        && saga.getCuentaId().equals(3L)));
    }

    @Test
    void debitar_conReferenciaYaProcesada_devuelveResultadoGuardadoSinVolverADescontar() {
        OperacionCuentaRequest request = new OperacionCuentaRequest(
                new BigDecimal("50.00"), "ref-ya-procesada");

        OperacionSaga operacionExistente = OperacionSaga.builder()
                .id(1L)
                .referencia("ref-ya-procesada")
                .tipoOperacion("DEBITO")
                .cuentaId(3L)
                .monto(new BigDecimal("50.00"))
                .saldoResultante(new BigDecimal("400.00"))
                .build();

        when(operacionSagaRepository.findByReferenciaAndTipoOperacion("ref-ya-procesada", "DEBITO"))
                .thenReturn(Mono.just(operacionExistente));

        StepVerifier.create(cuentaOperacionService.debitar(3L, request))
                .expectNextMatches(response ->
                        response.cuentaId().equals(3L)
                                && response.saldoResultante().compareTo(new BigDecimal("400.00")) == 0)
                .verifyComplete();

        verify(databaseClient, never()).sql(contains("UPDATE cuenta"));
        verify(operacionSagaRepository, never()).save(any());
    }

    @Test
    void debitar_conSaldoInsuficiente_lanzaSaldoInsuficiente() {
        OperacionCuentaRequest request = new OperacionCuentaRequest(
                new BigDecimal("999999.00"), "ref-sin-saldo");

        when(operacionSagaRepository.findByReferenciaAndTipoOperacion("ref-sin-saldo", "DEBITO"))
                .thenReturn(Mono.empty());

        GenericExecuteSpec updateDebito = mock(GenericExecuteSpec.class);
        when(databaseClient.sql(contains("UPDATE cuenta SET saldo = saldo - :monto"))).thenReturn(updateDebito);
        when(updateDebito.bind(anyString(), any())).thenReturn(updateDebito);
        when(updateDebito.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(0L));

        StepVerifier.create(cuentaOperacionService.debitar(3L, request))
                .expectErrorMatches(error -> error instanceof SaldoInsuficienteException)
                .verify();

        verify(operacionSagaRepository, never()).save(any());
    }
}