package com.banco.ms_auth.security;

import com.banco.ms_auth.entity.UsuarioAuth;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtService {

    private static final long EXPIRACION_MS = 3_600_000; // 1 hora

    private final JwtKeyProvider jwtKeyProvider;

    public String generarToken(UsuarioAuth usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusMillis(EXPIRACION_MS);

        return Jwts.builder()
                .header().keyId(jwtKeyProvider.getKeyId()).and()
                .subject(usuario.getUsername())
                .claim("clienteId", usuario.getClienteId())
                .claim("rol", usuario.getRol())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(jwtKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public long getExpiracionSegundos() {
        return EXPIRACION_MS / 1000;
    }
}
