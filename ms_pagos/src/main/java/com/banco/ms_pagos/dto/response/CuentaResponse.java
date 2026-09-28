package com.banco.ms_pagos.dto.response;

import java.math.BigDecimal;

public record CuentaResponse(Long id, String codigoUnico, Long clienteId, BigDecimal saldo, Boolean activo) {
}
