package com.banco.ms_cuentas.exception;

public class CuentaNoPerteneceAlClienteException extends RuntimeException {
    public CuentaNoPerteneceAlClienteException(String message) {
        super(message);
    }
}