package io.jetproxy.middleware.auth.jwk.validator;

public class FirebaseJwtValidator extends BaseJwtValidator {
    public FirebaseJwtValidator(String jwksUri, String expectedIssuer, String expectedAudience) {
        super("firebase", jwksUri, expectedIssuer, expectedAudience);
    }
}
