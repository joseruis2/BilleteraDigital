package com.banco.ms_cuentas.service;

import com.banco.ms_cuentas.config.ProductosConfig;
import com.banco.ms_cuentas.entity.Cuenta;
import com.banco.ms_cuentas.event.ClienteRegistradoPayload;
import com.banco.ms_cuentas.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    public Mono<Cuenta> crearCuentaAhorroDesdeEvento(ClienteRegistradoPayload payload) {
        Long productoId = ProductosConfig.PRODUCTO_CUENTA_AHORRO_ID;

        return cuentaRepository.existsByClienteIdAndProductoId(payload.clienteId(), productoId)
                .flatMap(existe -> {
                    if (existe) {
                        log.info("Cuenta de ahorro ya existe para clienteId={}, se ignora el evento duplicado",
                                payload.clienteId());
                        return Mono.empty();
                    }
                    return crear(payload, productoId);
                });
    }

    private Mono<Cuenta> crear(ClienteRegistradoPayload payload, Long productoId) {
        Cuenta cuenta = Cuenta.builder()
                .codigoUnico(generarCodigoUnico())
                .saldo(new BigDecimal("10.00"))
                .activo(true)
                .clienteId(payload.clienteId())
                .productoId(productoId)
                .build();

        return cuentaRepository.save(cuenta)
                .doOnSuccess(c -> log.info("Cuenta de ahorro creada para clienteId={}, cuentaId={}",
                        c.getClienteId(), c.getId()));
    }

    private String generarCodigoUnico() {
        return "CTA" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
