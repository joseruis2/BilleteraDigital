package com.banco.ms_pagos.dto.request;

import java.math.BigDecimal;

public record OperacionCuentaRequest(BigDecimal monto, String referencia) {
}
