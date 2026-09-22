package com.banco.ms_auth.repository;

import com.banco.ms_auth.entity.UsuarioAuth;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioAuthRepository extends ReactiveCrudRepository<UsuarioAuth, Long> {

    // Clave para la idempotencia: si el evento se reprocesa, no duplicamos el usuario_auth
    Mono<Boolean> existsByClienteId(Long clienteId);
}
