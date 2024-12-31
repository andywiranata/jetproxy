package io.jetproxy.middleware.auth.jwk.validator;

import com.nimbusds.jose.JWSObject;
import io.jetproxy.middleware.auth.jwk.JwkSource;
import io.jetproxy.middleware.auth.jwk.JwkSourceFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

public abstract class BaseJwtValidator {
    private final JwkSource jwkSource;
    private final String expectedIssuer;
    private final String expectedAudience;

    public BaseJwtValidator(String providerType, String jwksUri, String expectedIssuer, String expectedAudience) {
        this.jwkSource = JwkSourceFactory.createJwkSource(providerType, jwksUri);
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
    }

    public Claims validateToken(String token) throws Exception {
        String kid = extractKidFromJwt(token);
        RSAPublicKey publicKey = jwkSource.getPublicKey(kid);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        validateClaims(claims);
        return claims;
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

    private void validateClaims(Claims claims) {
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new IllegalArgumentException("Invalid issuer: " + claims.getIssuer());
        }
        if (!expectedAudience.equals(claims.getAudience())) {
            throw new IllegalArgumentException("Invalid audience: " + claims.getAudience());
        }
    }
}
