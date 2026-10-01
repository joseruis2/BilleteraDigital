package com.banco.ms_pagos.controller;


import com.banco.ms_pagos.dto.request.TransferenciaRequest;
import com.banco.ms_pagos.dto.response.DestinatarioResponse;
import com.banco.ms_pagos.dto.response.HistorialTransferenciaResponse;
import com.banco.ms_pagos.dto.response.TransferenciaResponse;
import com.banco.ms_pagos.service.DestinatarioService;
import com.banco.ms_pagos.service.TransferenciaOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaOrchestratorService transferenciaOrchestratorService;
    private final DestinatarioService destinatarioService;

    @PostMapping
    public Mono<TransferenciaResponse> transferir(@Valid @RequestBody TransferenciaRequest request,
                                                  JwtAuthenticationToken authentication) {
        return transferenciaOrchestratorService.ejecutar(request, extraerClienteId(authentication));
    }

    @GetMapping("/historial")
    public Flux<HistorialTransferenciaResponse> historial(JwtAuthenticationToken authentication) {
        return transferenciaOrchestratorService.obtenerHistorial(extraerClienteId(authentication));
    }

    @GetMapping("/destinatario")
    public Mono<DestinatarioResponse> buscarDestinatario(@RequestParam String numeroCuenta) {
        return destinatarioService.buscarPorNumeroCuenta(numeroCuenta);
    }

    private Long extraerClienteId(JwtAuthenticationToken authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number claim = jwt.getClaim("clienteId");
        return claim.longValue();
    }
}