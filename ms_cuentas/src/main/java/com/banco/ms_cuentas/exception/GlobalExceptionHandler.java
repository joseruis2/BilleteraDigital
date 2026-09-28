package com.banco.ms_cuentas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SaldoInsuficienteException.class)
    public Mono<ProblemDetail> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Saldo insuficiente");
        return Mono.just(pd);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public Mono<ProblemDetail> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Cuenta no encontrada");
        return Mono.just(pd);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidacion(WebExchangeBindException ex) {
        String detalle = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + " | " + b)
                .orElse("Datos invalidos");

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
        pd.setTitle("Error de validacion");
        return Mono.just(pd);
    }
}
