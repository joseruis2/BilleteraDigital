package com.banco.ms_auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("usuario_auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAuth {

    @Id
    private Long id;

    private String username;
    private String passwordHash;
    private Long clienteId;

    @Builder.Default
    private String rol = "CLIENTE";

    @Builder.Default
    private Boolean activo = true;

    @CreatedDate
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaModificacion;
}
