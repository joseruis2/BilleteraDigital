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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Table("limite_transaccional")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimiteTransaccional {

    @Id
    private Long id;

    private Long clienteId;
    private String tipoLimite; // DIARIO (por ahora es el unico que creamos)
    private BigDecimal montoLimite;

    @Builder.Default
    private BigDecimal montoUsado = BigDecimal.ZERO;

    private LocalDate fechaReferencia;

    @CreatedDate
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaModificacion;
}