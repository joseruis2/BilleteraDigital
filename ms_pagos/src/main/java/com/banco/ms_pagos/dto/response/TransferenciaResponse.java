package com.banco.ms_pagos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferenciaResponse(
        String codigoUnico,
        String estado,
        BigDecimal monto,
        String concepto,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        LocalDateTime fecha,
        String motivoFallo
) {
}
