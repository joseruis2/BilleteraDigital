package com.banco.ms_cliente.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteYaExisteException.class)
    public Mono<ProblemDetail> handleClienteYaExiste(ClienteYaExisteException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Cliente ya existe");
        return Mono.just(pd);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidacion(WebExchangeBindException ex) {
        String detalle = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + " | " + b)
                .orElse("Datos inválidos");

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
        pd.setTitle("Error de validación");
        return Mono.just(pd);
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public Mono<ProblemDetail> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Cliente no encontrado");
        return Mono.just(pd);
    }
}