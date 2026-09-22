package com.banco.ms_cliente.repository;


import com.banco.ms_cliente.entity.Cliente;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ClienteRepository extends ReactiveCrudRepository<Cliente, Long> {

    Mono<Boolean> existsByCorreo(String correo);

    Mono<Boolean> existsByCelular(String celular);

    Mono<Boolean> existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
