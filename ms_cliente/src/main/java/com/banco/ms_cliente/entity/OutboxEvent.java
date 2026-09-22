package com.banco.ms_cliente.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * NOTA: el campo payload se persiste como String (JSON serializado con Jackson)
 * porque el driver r2dbc-mysql no tiene un tipo Json nativo como el de Postgres.
 * MySQL guarda esto en la columna JSON sin problema al mandarlo como texto.
 */
@Table("outbox_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private Long id;

    private String aggregateType;
    private Long aggregateId;
    private String tipoEvento;
    private String payload; // JSON serializado

    @Builder.Default
    private Boolean publicado = false;

    @CreatedDate
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaPublicacion;
}
