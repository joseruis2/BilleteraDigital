package com.banco.ms_cuentas.dto.response;

import java.math.BigDecimal;

public record OperacionCuentaResponse(
        Long cuentaId,
        BigDecimal saldoResultante
) {
}
