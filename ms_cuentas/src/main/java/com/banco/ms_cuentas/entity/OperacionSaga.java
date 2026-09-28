package com.banco.ms_cuentas.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Garantiza idempotencia en las operaciones de debito/credito que pide el
 * orquestador de la saga (ms-pagos). Si la misma referencia+tipo llega dos
 * veces (por un reintento tras timeout), la segunda vez NO se vuelve a
 * mover el saldo: se devuelve el resultado ya guardado aqui.
 */
@Table("operacion_saga")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperacionSaga {

    @Id
    private Long id;

    private String referencia;      // normalmente el idempotency_key de la transferencia
    private String tipoOperacion;   // DEBITO | CREDITO | REVERSION_DEBITO
    private Long cuentaId;
    private BigDecimal monto;
    private BigDecimal saldoResultante;

    @CreatedDate
    private LocalDateTime fechaCreacion;
}
