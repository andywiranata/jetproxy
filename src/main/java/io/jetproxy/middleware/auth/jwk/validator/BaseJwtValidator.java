package io.jetproxy.middleware.auth.jwk.validator;

import com.nimbusds.jose.JWSObject;
import io.jetproxy.middleware.auth.jwk.JwkSource;
import io.jetproxy.middleware.auth.jwk.JwkSourceFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

public abstract class BaseJwtValidator {
    private final JwkSource jwkSource;

    public BaseJwtValidator(String providerType, String jwksUri, Long cacheTtl) {
        this.jwkSource = JwkSourceFactory.createJwkSource(providerType, jwksUri, cacheTtl);
    }
    public Claims validateTokenWithSigningKey(Key secretKey, String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public Claims validateTokenWithPublicKey(String token) throws Exception {
        String kid = extractKidFromJwt(token);
        RSAPublicKey publicKey = jwkSource.getPublicKey(kid);
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String extractKidFromJwt(String token) {
        JWSObject jwsObject = null;
        try {
            jwsObject = JWSObject.parse(token);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return jwsObject.getHeader().getKeyID();
    }
}
