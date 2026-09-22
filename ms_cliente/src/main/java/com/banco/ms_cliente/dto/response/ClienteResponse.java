package com.banco.ms_cliente.dto.response;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String codigoUnico,
        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String celular,
        String correo,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
