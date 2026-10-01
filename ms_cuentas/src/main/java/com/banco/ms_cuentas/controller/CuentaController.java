package com.banco.ms_cuentas.controller;


import com.banco.ms_cuentas.dto.request.OperacionCuentaRequest;
import com.banco.ms_cuentas.dto.response.CuentaResponse;
import com.banco.ms_cuentas.dto.response.OperacionCuentaResponse;
import com.banco.ms_cuentas.entity.Cuenta;
import com.banco.ms_cuentas.exception.CuentaNoEncontradaException;
import com.banco.ms_cuentas.exception.CuentaNoPerteneceAlClienteException;
import com.banco.ms_cuentas.repository.CuentaRepository;
import com.banco.ms_cuentas.service.CuentaOperacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Endpoints internos, pensados para ser llamados por ms-pagos (el orquestador
 * de la saga de transferencias), no directamente por el frontend.
 * En un entorno real, estos endpoints irian detras de una red interna / mTLS
 * o al menos un rol de servicio distinto al de un cliente final.
 */
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaRepository cuentaRepository;
    private final CuentaOperacionService cuentaOperacionService;

    @GetMapping("/buscar")
    public Mono<CuentaResponse> buscarPorNumeroCuenta(@RequestParam String numeroCuenta) {
        return cuentaRepository.findByCodigoUnico(numeroCuenta)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new CuentaNoEncontradaException(
                        "No existe una cuenta con el numero " + numeroCuenta)));
    }

    @GetMapping("/{id}")
    public Mono<CuentaResponse> obtenerPorId(@PathVariable Long id) {
        return cuentaRepository.findById(id)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new CuentaNoEncontradaException("No existe la cuenta " + id)));
    }

    @PostMapping("/{id}/debitar")
    public Mono<OperacionCuentaResponse> debitar(@PathVariable Long id,
                                                 @Valid @RequestBody OperacionCuentaRequest request) {
        return cuentaOperacionService.debitar(id, request);
    }

    @PostMapping("/{id}/acreditar")
    public Mono<OperacionCuentaResponse> acreditar(@PathVariable Long id,
                                                   @Valid @RequestBody OperacionCuentaRequest request) {
        return cuentaOperacionService.acreditar(id, request);
    }

    @PostMapping("/{id}/revertir-debito")
    public Mono<OperacionCuentaResponse> revertirDebito(@PathVariable Long id,
                                                        @Valid @RequestBody OperacionCuentaRequest request) {
        return cuentaOperacionService.revertirDebito(id, request);
    }

    private CuentaResponse toResponse(Cuenta cuenta) {
        return new CuentaResponse(cuenta.getId(), cuenta.getCodigoUnico(), cuenta.getClienteId(),
                cuenta.getSaldo(), cuenta.getActivo());
    }

    @GetMapping("/mias")
    public Flux<CuentaResponse> misCuentas(JwtAuthenticationToken authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number clienteIdClaim = jwt.getClaim("clienteId");
        Long clienteId = clienteIdClaim.longValue();

        return cuentaRepository.findByClienteId(clienteId)
                .map(this::toResponse);
    }

    @GetMapping("/{id}/saldo")
    public Mono<CuentaResponse> obtenerSaldoPropio(@PathVariable Long id, JwtAuthenticationToken authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number clienteIdClaim = jwt.getClaim("clienteId");
        Long clienteId = clienteIdClaim.longValue();

        return cuentaRepository.findById(id)
                .switchIfEmpty(Mono.error(new CuentaNoEncontradaException("No existe la cuenta " + id)))
                .flatMap(cuenta -> {
                    if (!cuenta.getClienteId().equals(clienteId)) {
                        return Mono.error(new CuentaNoPerteneceAlClienteException(
                                "La cuenta " + id + " no pertenece al cliente autenticado"));
                    }
                    return Mono.just(toResponse(cuenta));
                });
    }
}
