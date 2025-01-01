package io.jetproxy.middleware.auth.jwk.validator;

public class JwtValidator extends BaseJwtValidator{
    public JwtValidator(String providerType, String jwksUri,
                        Long cacheTtl) {
        super(providerType, jwksUri, cacheTtl);
    }
}
