package com.banco.ms_cliente.exception;

public class ClienteYaExisteException extends RuntimeException {
    public ClienteYaExisteException(String message) {
        super(message);
    }
}
