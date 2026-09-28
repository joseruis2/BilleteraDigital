package com.banco.ms_auth.controller;

import com.banco.ms_auth.security.JwtKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtKeyProvider jwtKeyProvider;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = jwtKeyProvider.getPublicKey();

        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sinBitDeSigno(publicKey.getModulus()));
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sinBitDeSigno(publicKey.getPublicExponent()));

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", jwtKeyProvider.getKeyId());
        jwk.put("n", n);
        jwk.put("e", e);

        return Map.of("keys", List.of(jwk));
    }

    // BigInteger.toByteArray() antepone un byte 0x00 si el numero se leeria
    // como negativo en complemento a dos; el JWK necesita el valor sin ese
    // byte extra o el modulo queda corrupto para quien lo consuma.
    private byte[] sinBitDeSigno(BigInteger valor) {
        byte[] bytes = valor.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}
