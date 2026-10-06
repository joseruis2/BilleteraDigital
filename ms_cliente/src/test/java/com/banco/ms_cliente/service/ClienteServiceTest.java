package com.banco.ms_cliente.service;

import com.banco.ms_cliente.dto.Request.ClienteRegistroRequest;
import com.banco.ms_cliente.dto.response.ClienteResponse;
import com.banco.ms_cliente.entity.Cliente;
import com.banco.ms_cliente.entity.OutboxEvent;
import com.banco.ms_cliente.exception.ClienteNoEncontradoException;
import com.banco.ms_cliente.exception.ClienteYaExisteException;
import com.banco.ms_cliente.mapper.ClienteMapper;
import com.banco.ms_cliente.repository.ClienteRepository;
import com.banco.ms_cliente.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private TransactionalOperator transactionalOperator;

    private ClienteService clienteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(
                clienteRepository, outboxEventRepository, clienteMapper, objectMapper, transactionalOperator);

        // El operador transaccional real abriria/cerraria una transaccion;
        // en el test simplemente dejamos pasar el Mono tal cual, sin
        // envolverlo en comportamiento transaccional real.
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ClienteRegistroRequest requestValido() {
        return new ClienteRegistroRequest(
                "Jose", "Ruiz Quispe", "DNI", "73967166",
                "912104890", "jose@hotmail.com", "joseruiz", "password123");
    }

    @Test
    void registrarCliente_conDatosNuevos_creaClienteYDevuelveResponse() {
        ClienteRegistroRequest request = requestValido();

        Cliente clienteSinGuardar = Cliente.builder()
                .nombres("Jose")
                .apellidos("Ruiz Quispe")
                .tipoDocumento("DNI")
                .numeroDocumento("73967166")
                .celular("912104890")
                .correo("jose@hotmail.com")
                .build();

        Cliente clienteGuardado = Cliente.builder()
                .id(1L)
                .codigoUnico("CLI123456789012")
                .nombres("Jose")
                .apellidos("Ruiz Quispe")
                .tipoDocumento("DNI")
                .numeroDocumento("73967166")
                .celular("912104890")
                .correo("jose@hotmail.com")
                .activo(true)
                .build();

        ClienteResponse responseEsperado = new ClienteResponse(
                1L, "CLI123456789012", "Jose", "Ruiz Quispe", "DNI",
                "73967166", "912104890", "jose@hotmail.com", true, null);

        when(clienteRepository.existsByCorreo("jose@hotmail.com")).thenReturn(Mono.just(false));
        when(clienteRepository.existsByCelular("912104890")).thenReturn(Mono.just(false));
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento("DNI", "73967166"))
                .thenReturn(Mono.just(false));

        when(clienteMapper.toEntity(request)).thenReturn(clienteSinGuardar);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(Mono.just(clienteGuardado));
        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.just(OutboxEvent.builder().id(1L).build()));
        when(clienteMapper.toResponse(clienteGuardado)).thenReturn(responseEsperado);

        StepVerifier.create(clienteService.registrarCliente(request))
                .expectNext(responseEsperado)
                .verifyComplete();

        // Confirma que el evento se guardo con el tipo correcto, para el outbox relay
        verify(outboxEventRepository).save(argThat(event ->
                "CLIENTE_REGISTRADO".equals(event.getTipoEvento())
                        && "CLIENTE".equals(event.getAggregateType())
                        && Boolean.FALSE.equals(event.getPublicado())));
    }

    @Test
    void registrarCliente_conCorreoYaExistente_lanzaClienteYaExiste() {
        ClienteRegistroRequest request = requestValido();

        when(clienteRepository.existsByCorreo("jose@hotmail.com")).thenReturn(Mono.just(true));

        StepVerifier.create(clienteService.registrarCliente(request))
                .expectErrorMatches(error ->
                        error instanceof ClienteYaExisteException
                                && error.getMessage().contains("correo"))
                .verify();

        // No debe seguir validando celular/documento si ya fallo por correo
        verify(clienteRepository, never()).existsByCelular(any());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarCliente_conCelularYaExistente_lanzaClienteYaExiste() {
        ClienteRegistroRequest request = requestValido();

        when(clienteRepository.existsByCorreo("jose@hotmail.com")).thenReturn(Mono.just(false));
        when(clienteRepository.existsByCelular("912104890")).thenReturn(Mono.just(true));

        StepVerifier.create(clienteService.registrarCliente(request))
                .expectErrorMatches(error ->
                        error instanceof ClienteYaExisteException
                                && error.getMessage().contains("celular"))
                .verify();

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarCliente_conDocumentoYaExistente_lanzaClienteYaExiste() {
        ClienteRegistroRequest request = requestValido();

        when(clienteRepository.existsByCorreo("jose@hotmail.com")).thenReturn(Mono.just(false));
        when(clienteRepository.existsByCelular("912104890")).thenReturn(Mono.just(false));
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento("DNI", "73967166"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(clienteService.registrarCliente(request))
                .expectErrorMatches(error ->
                        error instanceof ClienteYaExisteException
                                && error.getMessage().contains("documento"))
                .verify();

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void obtenerPerfil_conClienteExistente_devuelveResponse() {
        Cliente cliente = Cliente.builder().id(3L).nombres("Percy").build();
        ClienteResponse responseEsperado = new ClienteResponse(
                3L, "CLI...", "Percy", "Quispe", "DNI", "33367169",
                "112104890", "percy@hotmail.com", true, null);

        when(clienteRepository.findById(3L)).thenReturn(Mono.just(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(responseEsperado);

        StepVerifier.create(clienteService.obtenerPerfil(3L))
                .expectNext(responseEsperado)
                .verifyComplete();
    }

    @Test
    void obtenerPerfil_conClienteInexistente_lanzaClienteNoEncontrado() {
        when(clienteRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(clienteService.obtenerPerfil(999L))
                .expectErrorMatches(error ->
                        error instanceof ClienteNoEncontradoException
                                && error.getMessage().contains("999"))
                .verify();
    }
}
