package com.banco.ms_pagos.dto.response;

import java.math.BigDecimal;

public record OperacionCuentaResponse(Long cuentaId, BigDecimal saldoResultante) {
}
