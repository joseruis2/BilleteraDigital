package com.banco.ms_pagos.service;

import com.banco.ms_pagos.client.ClienteClient;
import com.banco.ms_pagos.client.CuentaClient;
import com.banco.ms_pagos.dto.request.TransferenciaRequest;
import com.banco.ms_pagos.dto.response.ClienteInternoResponse;
import com.banco.ms_pagos.dto.response.CuentaResponse;
import com.banco.ms_pagos.dto.response.OperacionCuentaResponse;
import com.banco.ms_pagos.dto.response.TransferenciaResponse;
import com.banco.ms_pagos.entity.Transferencia;
import com.banco.ms_pagos.exception.CuentaNoPerteneceAlClienteException;
import com.banco.ms_pagos.exception.SaldoInsuficienteException;
import com.banco.ms_pagos.repository.TransferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaOrchestratorServiceTest {

    @Mock
    private TransferenciaRepository transferenciaRepository;

    @Mock
    private LimiteTransaccionalService limiteTransaccionalService;

    @Mock
    private CuentaClient cuentaClient;

    @Mock
    private ClienteClient clienteClient;

    private TransferenciaOrchestratorService orchestratorService;

    private final CuentaResponse cuentaOrigen = new CuentaResponse(
            1L, "CTA_ORIGEN", 10L, new BigDecimal("100.00"), true);

    private final CuentaResponse cuentaDestino = new CuentaResponse(
            2L, "CTA_DESTINO", 20L, new BigDecimal("50.00"), true);

    private final ClienteInternoResponse clienteDestino = new ClienteInternoResponse(
            20L, "Ana", "Torres");

    @BeforeEach
    void setUp() {
        orchestratorService = new TransferenciaOrchestratorService(
                transferenciaRepository, limiteTransaccionalService, cuentaClient, clienteClient);

        // Simula que el repositorio "guarda y devuelve" el mismo objeto,
        // asignandole un id la primera vez que se guarda sin uno.
        lenient().when(transferenciaRepository.save(any(Transferencia.class)))
                .thenAnswer(invocation -> {
                    Transferencia t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(99L);
                    }
                    return Mono.just(t);
                });
    }

    private TransferenciaRequest requestValido() {
        return new TransferenciaRequest(1L, "CTA_DESTINO", new BigDecimal("30.00"), "Prueba");
    }

    @Test
    void ejecutar_cuentaOrigenNoPerteneceAlClienteAutenticado_lanzaExcepcionSinTocarNada() {
        when(cuentaClient.obtenerPorId(1L)).thenReturn(Mono.just(cuentaOrigen));

        // clienteIdAutenticado = 999, pero la cuenta origen es del cliente 10
        StepVerifier.create(orchestratorService.ejecutar(requestValido(), 999L))
                .expectErrorMatches(error -> error instanceof CuentaNoPerteneceAlClienteException)
                .verify();

        verify(cuentaClient, never()).buscarPorNumeroCuenta(any());
        verify(transferenciaRepository, never()).save(any());
        verify(limiteTransaccionalService, never()).reservarLimite(any(), any());
    }

    @Test
    void ejecutar_caminoFeliz_debitaAcreditaYCompletaConNombreDestinatario() {
        when(cuentaClient.obtenerPorId(1L)).thenReturn(Mono.just(cuentaOrigen));
        when(cuentaClient.buscarPorNumeroCuenta("CTA_DESTINO")).thenReturn(Mono.just(cuentaDestino));
        when(clienteClient.obtenerPorId(20L)).thenReturn(Mono.just(clienteDestino));

        when(limiteTransaccionalService.reservarLimite(10L, new BigDecimal("30.00")))
                .thenReturn(Mono.empty());

        when(cuentaClient.debitar(eq(1L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.just(new OperacionCuentaResponse(1L, new BigDecimal("70.00"))));
        when(cuentaClient.acreditar(eq(2L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.just(new OperacionCuentaResponse(2L, new BigDecimal("80.00"))));

        StepVerifier.create(orchestratorService.ejecutar(requestValido(), 10L))
                .expectNextMatches(response ->
                        "COMPLETADA".equals(response.estado())
                                && "Ana Torres".equals(response.nombreDestinatario())
                                && response.motivoFallo() == null)
                .verifyComplete();

        // La saga nunca debe intentar compensar en el camino feliz
        verify(cuentaClient, never()).revertirDebito(any(), any(), any());
        verify(limiteTransaccionalService, never()).liberarLimite(any(), any());
    }

    @Test
    void ejecutar_debitoFalla_liberaLimiteYMarcaFallidaSinRevertirDebito() {
        when(cuentaClient.obtenerPorId(1L)).thenReturn(Mono.just(cuentaOrigen));
        when(cuentaClient.buscarPorNumeroCuenta("CTA_DESTINO")).thenReturn(Mono.just(cuentaDestino));
        when(clienteClient.obtenerPorId(20L)).thenReturn(Mono.just(clienteDestino));

        when(limiteTransaccionalService.reservarLimite(10L, new BigDecimal("30.00")))
                .thenReturn(Mono.empty());

        when(cuentaClient.debitar(eq(1L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.error(new SaldoInsuficienteException("Saldo insuficiente en la cuenta origen")));

        when(limiteTransaccionalService.liberarLimite(1L, new BigDecimal("30.00")))
                .thenReturn(Mono.empty());

        StepVerifier.create(orchestratorService.ejecutar(requestValido(), 10L))
                .expectNextMatches(response ->
                        "FALLIDA".equals(response.estado())
                                && response.motivoFallo() != null
                                && response.motivoFallo().contains("Saldo insuficiente"))
                .verifyComplete();

        // Si el debito nunca se aplico, no hay nada que revertir ni acreditar
        verify(cuentaClient, never()).revertirDebito(any(), any(), any());
        verify(cuentaClient, never()).acreditar(any(), any(), any());
        verify(limiteTransaccionalService).liberarLimite(1L, new BigDecimal("30.00"));
    }

    @Test
    void ejecutar_creditoFalla_compensaRevirtiendoDebitoYLiberandoLimite() {
        when(cuentaClient.obtenerPorId(1L)).thenReturn(Mono.just(cuentaOrigen));
        when(cuentaClient.buscarPorNumeroCuenta("CTA_DESTINO")).thenReturn(Mono.just(cuentaDestino));
        when(clienteClient.obtenerPorId(20L)).thenReturn(Mono.just(clienteDestino));

        when(limiteTransaccionalService.reservarLimite(10L, new BigDecimal("30.00")))
                .thenReturn(Mono.empty());

        when(cuentaClient.debitar(eq(1L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.just(new OperacionCuentaResponse(1L, new BigDecimal("70.00"))));

        // El credito al destino falla (por ejemplo, cuenta destino inactiva)
        when(cuentaClient.acreditar(eq(2L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.error(new RuntimeException("Cuenta destino no encontrada")));

        when(cuentaClient.revertirDebito(eq(1L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.just(new OperacionCuentaResponse(1L, new BigDecimal("100.00"))));

        when(limiteTransaccionalService.liberarLimite(1L, new BigDecimal("30.00")))
                .thenReturn(Mono.empty());

        StepVerifier.create(orchestratorService.ejecutar(requestValido(), 10L))
                .expectNextMatches(response ->
                        "FALLIDA".equals(response.estado())
                                && response.motivoFallo() != null)
                .verifyComplete();

        // La compensacion completa debe haber ocurrido: revertir debito + liberar limite
        verify(cuentaClient).revertirDebito(eq(1L), anyString(), eq(new BigDecimal("30.00")));
        verify(limiteTransaccionalService).liberarLimite(1L, new BigDecimal("30.00"));
    }

    @Test
    void ejecutar_siMsClienteNoResponde_usaNombreGenericoYNoAbortaLaTransferencia() {
        when(cuentaClient.obtenerPorId(1L)).thenReturn(Mono.just(cuentaOrigen));
        when(cuentaClient.buscarPorNumeroCuenta("CTA_DESTINO")).thenReturn(Mono.just(cuentaDestino));

        // ms-cliente esta caido / responde error
        when(clienteClient.obtenerPorId(20L)).thenReturn(Mono.error(new RuntimeException("timeout")));

        when(limiteTransaccionalService.reservarLimite(10L, new BigDecimal("30.00")))
                .thenReturn(Mono.empty());
        when(cuentaClient.debitar(eq(1L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.just(new OperacionCuentaResponse(1L, new BigDecimal("70.00"))));
        when(cuentaClient.acreditar(eq(2L), anyString(), eq(new BigDecimal("30.00"))))
                .thenReturn(Mono.just(new OperacionCuentaResponse(2L, new BigDecimal("80.00"))));

        StepVerifier.create(orchestratorService.ejecutar(requestValido(), 10L))
                .expectNextMatches(response ->
                        "COMPLETADA".equals(response.estado())
                                && "Destinatario".equals(response.nombreDestinatario()))
                .verifyComplete();
    }
}
