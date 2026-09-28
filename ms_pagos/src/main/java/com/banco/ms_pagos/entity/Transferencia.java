package com.banco.ms_pagos.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("transferencia")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transferencia {

    @Id
    private Long id;

    private String codigoUnico;
    private String idempotencyKey;
    private String sagaId;
    private Long cuentaOrigenId;
    private Long clienteOrigenId;
    private Long cuentaDestinoId;
    private BigDecimal monto;
    private String concepto;
    private String estado;      // INICIADA | COMPLETADA | FALLIDA
    private String pasoSaga;    // VALIDANDO_LIMITE | DEBITANDO_ORIGEN | ACREDITANDO_DESTINO | COMPENSANDO | FINALIZADA
    private String motivoFallo;

    @CreatedDate
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaModificacion;
}
