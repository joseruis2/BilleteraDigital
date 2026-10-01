package com.banco.ms_pagos.dto.response;

public record DestinatarioResponse(
        Long cuentaId,
        String numeroCuentaMascarado,
        String nombreCompleto
) {
}
