package com.banco.ms_auth.security;

import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Genera el par de llaves RSA al arrancar la app y lo mantiene en memoria.
 *
 * OJO: esto es valido para desarrollo. En produccion las llaves deben
 * persistir (PKCS12/JKS en disco, o un KMS) porque:
 *  - Si reinicias ms-auth, todos los tokens emitidos antes quedan invalidos
 *    (la llave publica publicada en el JWKS cambia).
 *  - Si corres mas de una instancia de ms-auth detras de un load balancer,
 *    cada una generaria su propia llave y los demas microservicios
 *    fallarian al validar tokens firmados por la instancia "equivocada".
 */
@Component
public class JwtKeyProvider {

    private static final String KEY_ID = "ms-auth-key-1";

    private final KeyPair keyPair;

    public JwtKeyProvider() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el par de llaves RSA para JWT", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    public String getKeyId() {
        return KEY_ID;
    }
}