package com.banco.ms_cliente.event;

/**
 * Payload que consumen ms-auth (crea usuario_auth) y ms-cuentas (crea cuenta
 * con producto CUENTA_AHORRO por defecto). Viaja dentro de outbox_event.payload.
 *
 * ms-auth usa clienteId + username + password para crear el usuario_auth
 * (el hash de password se hace en ms-auth, no aquí).
 * ms-cuentas usa clienteId para crear la cuenta de ahorro por defecto.
 */
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
