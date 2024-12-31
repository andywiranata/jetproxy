package io.jetproxy.middleware.auth.jwk.validator;

public class Auth0JwtValidator extends BaseJwtValidator {
    public Auth0JwtValidator(String jwksUri, String expectedIssuer, String expectedAudience) {
        super("generic", jwksUri);
    }
}
