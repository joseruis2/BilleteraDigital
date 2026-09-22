package com.banco.ms_pagos.event;

public record ClienteRegistradoPayload(
        Long clienteId,
        String codigoUnico,
        String nombres,
        String apellidos,
        String correo,
        String username,
        String password
) {
}
