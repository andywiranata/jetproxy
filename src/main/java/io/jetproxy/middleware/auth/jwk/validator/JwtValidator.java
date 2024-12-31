package io.jetproxy.middleware.auth.jwk.validator;

public class JwtValidator extends BaseJwtValidator{
    public JwtValidator(String providerType, String jwksUri, String expectedIssuer, String expectedAudience) {
        super(providerType, jwksUri, expectedIssuer, expectedAudience);
    }
}
