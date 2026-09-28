package com.banco.ms_pagos.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LimiteTransaccionalResponse(
        BigDecimal montoLimite,
        BigDecimal montoUsado,
        BigDecimal montoDisponible,
        List<MovimientoLimiteResponse> historialDelDia
) {
}