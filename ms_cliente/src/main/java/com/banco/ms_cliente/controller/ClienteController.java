package com.banco.ms_cliente.controller;


import com.banco.ms_cliente.dto.Request.ClienteRegistroRequest;
import com.banco.ms_cliente.dto.response.ClienteResponse;
import com.banco.ms_cliente.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Clientes")
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Registra un cliente nuevo (dispara creación de auth y cuenta vía Kafka)")
    @PostMapping("/registro")
    @PreAuthorize("permitAll()") // endpoint público, no requiere JWT
    public Mono<ResponseEntity<ClienteResponse>> registrar(@Valid @RequestBody ClienteRegistroRequest request) {
        return clienteService.registrarCliente(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}