package com.banco.ms_auth.repository;

import com.banco.ms_auth.entity.UsuarioAuth;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioAuthRepository extends ReactiveCrudRepository<UsuarioAuth, Long> {

    Mono<Boolean> existsByClienteId(Long clienteId);

    Mono<UsuarioAuth> findByUsername(String username);
}
