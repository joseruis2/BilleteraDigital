package com.banco.ms_cliente.service;


import com.banco.ms_cliente.dto.Request.ClienteRegistroRequest;
import com.banco.ms_cliente.dto.response.ClienteResponse;
import com.banco.ms_cliente.entity.Cliente;
import com.banco.ms_cliente.entity.OutboxEvent;
import com.banco.ms_cliente.event.ClienteRegistradoPayload;
import com.banco.ms_cliente.exception.ClienteNoEncontradoException;
import com.banco.ms_cliente.exception.ClienteYaExisteException;
import com.banco.ms_cliente.mapper.ClienteMapper;
import com.banco.ms_cliente.repository.ClienteRepository;
import com.banco.ms_cliente.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ClienteMapper clienteMapper;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;

    public Mono<ClienteResponse> registrarCliente(ClienteRegistroRequest request) {
        return validarUnicidad(request)
                .then(Mono.defer(() -> guardarClienteConEvento(request)))
                .as(transactionalOperator::transactional)
                .map(clienteMapper::toResponse);
    }

    private Mono<Void> validarUnicidad(ClienteRegistroRequest request) {
        return clienteRepository.existsByCorreo(request.correo())
                .flatMap(existe -> existe
                        ? Mono.error(new ClienteYaExisteException("Ya existe un cliente con el correo " + request.correo()))
                        : clienteRepository.existsByCelular(request.celular()))
                .flatMap(existe -> existe
                        ? Mono.error(new ClienteYaExisteException("Ya existe un cliente con el celular " + request.celular()))
                        : clienteRepository.existsByTipoDocumentoAndNumeroDocumento(
                        request.tipoDocumento(), request.numeroDocumento()))
                .flatMap(existe -> existe
                        ? Mono.<Void>error(new ClienteYaExisteException(
                        "Ya existe un cliente con documento " + request.numeroDocumento()))
                        : Mono.empty());
    }

    private Mono<Cliente> guardarClienteConEvento(ClienteRegistroRequest request) {
        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setCodigoUnico(generarCodigoUnico());
        cliente.setActivo(true);

        return clienteRepository.save(cliente)
                .flatMap(clienteGuardado -> publicarEventoClienteRegistrado(clienteGuardado, request)
                        .thenReturn(clienteGuardado));
    }

    private Mono<OutboxEvent> publicarEventoClienteRegistrado(Cliente cliente, ClienteRegistroRequest request) {
        ClienteRegistradoPayload payload = new ClienteRegistradoPayload(
                cliente.getId(),
                cliente.getCodigoUnico(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getCorreo(),
                request.username(),
                request.password()
        );

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializando payload de ClienteRegistrado para cliente {}", cliente.getId(), e);
            return Mono.error(new IllegalStateException("No se pudo serializar el evento ClienteRegistrado", e));
        }

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("CLIENTE")
                .aggregateId(cliente.getId())
                .tipoEvento("CLIENTE_REGISTRADO")
                .payload(payloadJson)
                .publicado(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return outboxEventRepository.save(event);
    }

    private String generarCodigoUnico() {
        return "CLI" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public Mono<ClienteResponse> obtenerPerfil(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .switchIfEmpty(Mono.error(new ClienteNoEncontradoException(
                        "No existe el cliente " + clienteId)))
                .map(clienteMapper::toResponse);
    }
}
