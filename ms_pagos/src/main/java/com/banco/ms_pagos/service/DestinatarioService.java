package com.banco.ms_pagos.service;

import com.banco.ms_pagos.client.ClienteClient;
import com.banco.ms_pagos.client.CuentaClient;
import com.banco.ms_pagos.dto.response.DestinatarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DestinatarioService {

    private final CuentaClient cuentaClient;
    private final ClienteClient clienteClient;

    public Mono<DestinatarioResponse> buscarPorNumeroCuenta(String numeroCuenta) {
        return cuentaClient.buscarPorNumeroCuenta(numeroCuenta)
                .flatMap(cuenta -> clienteClient.obtenerPorId(cuenta.clienteId())
                        .map(cliente -> new DestinatarioResponse(
                                cuenta.id(),
                                enmascarar(cuenta.codigoUnico()),
                                (cliente.nombres() + " " + cliente.apellidos()).trim())));
    }

    private String enmascarar(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.length() <= 4) {
            return numeroCuenta;
        }
        return "*".repeat(numeroCuenta.length() - 4)
                + numeroCuenta.substring(numeroCuenta.length() - 4);
    }
}
