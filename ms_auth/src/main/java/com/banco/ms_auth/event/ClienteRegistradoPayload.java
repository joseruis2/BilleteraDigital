package com.banco.ms_auth.event;

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