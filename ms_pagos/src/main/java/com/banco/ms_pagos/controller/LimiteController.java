package com.banco.ms_pagos.controller;

import com.banco.ms_pagos.dto.response.LimiteTransaccionalResponse;
import com.banco.ms_pagos.service.LimiteTransaccionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/limites")
@RequiredArgsConstructor
public class LimiteController {

    private final LimiteTransaccionalService limiteTransaccionalService;

    @GetMapping
    public Mono<LimiteTransaccionalResponse> obtenerLimiteDelDia(JwtAuthenticationToken authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number clienteIdClaim = jwt.getClaim("clienteId");
        Long clienteId = clienteIdClaim.longValue();

        return limiteTransaccionalService.obtenerLimiteDelDia(clienteId);
    }
}