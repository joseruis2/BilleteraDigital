package com.banco.ms_auth.service;

import com.banco.ms_auth.entity.UsuarioAuth;
import com.banco.ms_auth.event.ClienteRegistradoPayload;
import com.banco.ms_auth.repository.UsuarioAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioAuthService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public Mono<UsuarioAuth> crearUsuarioDesdeEvento(ClienteRegistradoPayload payload) {
        return usuarioAuthRepository.existsByClienteId(payload.clienteId())
                .flatMap(existe -> {
                    if (existe) {
                        // Evento reprocesado (at-least-once delivery): no duplicamos, solo logueamos.
                        log.info("usuario_auth ya existe para clienteId={}, se ignora el evento duplicado",
                                payload.clienteId());
                        return Mono.empty();
                    }
                    return crear(payload);
                });
    }

    private Mono<UsuarioAuth> crear(ClienteRegistradoPayload payload) {
        UsuarioAuth usuario = UsuarioAuth.builder()
                .username(payload.username())
                .passwordHash(passwordEncoder.encode(payload.password()))
                .clienteId(payload.clienteId())
                .rol("CLIENTE")
                .activo(true)
                .build();

        return usuarioAuthRepository.save(usuario)
                .doOnSuccess(u -> log.info("usuario_auth creado para clienteId={}, username={}",
                        u.getClienteId(), u.getUsername()));
    }
}