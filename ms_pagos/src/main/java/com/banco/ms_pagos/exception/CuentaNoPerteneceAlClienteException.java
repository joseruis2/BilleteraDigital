package com.banco.ms_pagos.exception;

public class CuentaNoPerteneceAlClienteException extends RuntimeException {
    public CuentaNoPerteneceAlClienteException(String message) {
        super(message);
    }
}
