package com.banco.ms_auth.service;

import com.banco.ms_auth.dto.request.LoginRequest;
import com.banco.ms_auth.dto.response.LoginResponse;
import com.banco.ms_auth.entity.UsuarioAuth;
import com.banco.ms_auth.exception.CredencialesInvalidasException;
import com.banco.ms_auth.mapper.AuthMapper;
import com.banco.ms_auth.repository.UsuarioAuthRepository;
import com.banco.ms_auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthMapper authMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioAuthRepository, passwordEncoder, jwtService, authMapper);
    }

    @Test
    void login_conCredencialesValidas_devuelveTokenYDatosDelUsuario() {
        UsuarioAuth usuario = UsuarioAuth.builder()
                .id(1L)
                .username("joseruiz")
                .passwordHash("hash-encriptado")
                .clienteId(1L)
                .rol("CLIENTE")
                .activo(true)
                .build();

        LoginRequest request = new LoginRequest("joseruiz", "password123");
        LoginResponse respuestaEsperada = new LoginResponse(
                "Bearer", "token-falso-123", 3600L, "joseruiz", "CLIENTE");

        when(usuarioAuthRepository.findByUsername("joseruiz")).thenReturn(Mono.just(usuario));
        when(passwordEncoder.matches("password123", "hash-encriptado")).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token-falso-123");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);
        when(authMapper.toLoginResponse(usuario, "token-falso-123", 3600L)).thenReturn(respuestaEsperada);

        StepVerifier.create(authService.login(request))
                .expectNext(respuestaEsperada)
                .verifyComplete();
    }

    @Test
    void login_conUsuarioInexistente_lanzaCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("usuarioQueNoExiste", "password123");

        when(usuarioAuthRepository.findByUsername("usuarioQueNoExiste")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof CredencialesInvalidasException
                                && error.getMessage().equals("Usuario o contraseña incorrectos"))
                .verify();
    }

    @Test
    void login_conPasswordIncorrecto_lanzaCredencialesInvalidas() {
        UsuarioAuth usuario = UsuarioAuth.builder()
                .id(1L)
                .username("joseruiz")
                .passwordHash("hash-encriptado")
                .clienteId(1L)
                .rol("CLIENTE")
                .activo(true)
                .build();

        LoginRequest request = new LoginRequest("joseruiz", "password-incorrecta");

        when(usuarioAuthRepository.findByUsername("joseruiz")).thenReturn(Mono.just(usuario));
        when(passwordEncoder.matches("password-incorrecta", "hash-encriptado")).thenReturn(false);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof CredencialesInvalidasException
                                && error.getMessage().equals("Usuario o contraseña incorrectos"))
                .verify();
    }

    @Test
    void login_conUsuarioInactivo_lanzaCredencialesInvalidas() {
        UsuarioAuth usuarioInactivo = UsuarioAuth.builder()
                .id(1L)
                .username("joseruiz")
                .passwordHash("hash-encriptado")
                .clienteId(1L)
                .rol("CLIENTE")
                .activo(false)
                .build();

        LoginRequest request = new LoginRequest("joseruiz", "password123");

        when(usuarioAuthRepository.findByUsername("joseruiz")).thenReturn(Mono.just(usuarioInactivo));

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof CredencialesInvalidasException
                                && error.getMessage().equals("El usuario está inactivo"))
                .verify();

        // El password nunca deberia revisarse si el usuario ya esta inactivo
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}