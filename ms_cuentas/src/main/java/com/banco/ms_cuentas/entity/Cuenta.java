package com.banco.ms_cuentas.entity;

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

@Table("cuenta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {

    @Id
    private Long id;

    private String codigoUnico;

    @Builder.Default
    private BigDecimal saldo = new BigDecimal("10.00");

    @Builder.Default
    private Boolean activo = true;

    private Long clienteId;
    private Long productoId;

    @CreatedDate
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaModificacion;
}

