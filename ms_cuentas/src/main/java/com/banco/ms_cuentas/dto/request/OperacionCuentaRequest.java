package com.banco.ms_cuentas.dto.request;



import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OperacionCuentaRequest(

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal monto,

        // Idempotency key de la transferencia (viene de ms-pagos). Si esta misma
        // referencia + tipo de operacion llega de nuevo, no se vuelve a mover el saldo.
        @NotBlank(message = "La referencia es obligatoria")
        String referencia
) {
}

