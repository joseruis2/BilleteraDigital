package com.banco.ms_pagos.controller;


import com.banco.ms_pagos.dto.request.TransferenciaRequest;
import com.banco.ms_pagos.dto.response.HistorialTransferenciaResponse;
import com.banco.ms_pagos.dto.response.TransferenciaResponse;
import com.banco.ms_pagos.repository.LimiteTransaccionalRepository;
import com.banco.ms_pagos.repository.TransferenciaRepository;
import com.banco.ms_pagos.service.TransferenciaOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaOrchestratorService transferenciaOrchestratorService;
    private final LimiteTransaccionalRepository limiteTransaccionalRepository;
    private final DatabaseClient databaseClient;
    private final TransferenciaRepository transferenciaRepository;

    @PostMapping
    public Mono<TransferenciaResponse> transferir(@Valid @RequestBody TransferenciaRequest request,
                                                  JwtAuthenticationToken authentication) {
        Long clienteId = extraerClienteId(authentication);
        return transferenciaOrchestratorService.ejecutar(request, clienteId);
    }

    @GetMapping("/historial")
    public Flux<HistorialTransferenciaResponse> historial(JwtAuthenticationToken authentication) {
        Long clienteId = extraerClienteId(authentication);
        return transferenciaOrchestratorService.obtenerHistorial(clienteId);
    }

    private Long extraerClienteId(JwtAuthenticationToken authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number clienteIdClaim = jwt.getClaim("clienteId");
        return clienteIdClaim.longValue();
    }
}