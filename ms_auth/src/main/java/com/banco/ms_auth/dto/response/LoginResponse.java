package com.banco.ms_auth.dto.response;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String username,
        String rol
) {
}
