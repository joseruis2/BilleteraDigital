package com.banco.ms_auth.mapper;

import com.banco.ms_auth.dto.response.LoginResponse;
import com.banco.ms_auth.entity.UsuarioAuth;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "accessToken", source = "token")
    @Mapping(target = "expiresIn", source = "expiracionSegundos")
    @Mapping(target = "username", source = "usuario.username")
    @Mapping(target = "rol", source = "usuario.rol")
    LoginResponse toLoginResponse(UsuarioAuth usuario, String token, long expiracionSegundos);
}