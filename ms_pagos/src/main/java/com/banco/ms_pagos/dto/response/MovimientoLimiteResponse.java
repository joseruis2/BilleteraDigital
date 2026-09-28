package com.banco.ms_pagos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoLimiteResponse(
        String codigoUnico,
        String concepto,
        BigDecimal monto,
        LocalDateTime fecha,
        String estado
) {
}
