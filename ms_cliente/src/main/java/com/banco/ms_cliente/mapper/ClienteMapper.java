package com.banco.ms_cliente.mapper;


import com.banco.ms_cliente.dto.Request.ClienteRegistroRequest;
import com.banco.ms_cliente.dto.response.ClienteResponse;
import com.banco.ms_cliente.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codigoUnico", ignore = true) // se genera en el service
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    Cliente toEntity(ClienteRegistroRequest request);

    ClienteResponse toResponse(Cliente cliente);
}
