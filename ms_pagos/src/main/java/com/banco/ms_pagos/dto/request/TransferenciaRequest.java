package com.banco.ms_pagos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(

        @NotNull(message = "La cuenta origen es obligatoria")
        Long cuentaOrigenId,

        @NotBlank(message = "El numero de cuenta destino es obligatorio")
        String numeroCuentaDestino,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal monto,

        String concepto
) {
}
