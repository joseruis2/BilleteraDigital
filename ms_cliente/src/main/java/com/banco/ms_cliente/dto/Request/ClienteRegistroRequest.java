package com.banco.ms_cliente.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombres,

        @NotBlank(message = "El apellido es obligatorio")
        String apellidos,

        @NotBlank(message = "El tipo de documento es obligatorio")
        String tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String numeroDocumento,

        @NotBlank(message = "El celular es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos")
        String celular,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        // Username para login; se manda tal cual a ms-auth vía el evento
        @NotBlank(message = "El usuario es obligatorio")
        String username,

        // Contraseña en texto plano: ms-cliente NO la hashea ni la persiste,
        // solo la reenvía dentro del payload del evento hacia ms-auth
        // (el tópico de Kafka debe ir cifrado con TLS por este motivo).
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password
) {
}
