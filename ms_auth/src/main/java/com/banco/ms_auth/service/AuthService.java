package com.banco.ms_auth.service;

import com.banco.ms_auth.dto.request.LoginRequest;
import com.banco.ms_auth.dto.response.LoginResponse;
import com.banco.ms_auth.exception.CredencialesInvalidasException;
import com.banco.ms_auth.repository.UsuarioAuthRepository;
import com.banco.ms_auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Mono<LoginResponse> login(LoginRequest request) {
        return usuarioAuthRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new CredencialesInvalidasException("Usuario o contraseña incorrectos")))
                .flatMap(usuario -> {
                    if (!Boolean.TRUE.equals(usuario.getActivo())) {
                        return Mono.error(new CredencialesInvalidasException("El usuario está inactivo"));
                    }
                    if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
                        return Mono.error(new CredencialesInvalidasException("Usuario o contraseña incorrectos"));
                    }

                    String token = jwtService.generarToken(usuario);
                    return Mono.just(new LoginResponse(
                            "Bearer",
                            token,
                            jwtService.getExpiracionSegundos(),
                            usuario.getUsername(),
                            usuario.getRol()
                    ));
                });
    }
}
