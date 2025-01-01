package io.jetproxy.middleware.auth.jwk;

import io.jetproxy.middleware.auth.jwk.source.JwkJsonSource;
import io.jetproxy.middleware.auth.jwk.source.JwkX509Source;

public class JwkSourceFactory {

    /**
     * Provides an instance of JwkSource based on the specified source type and JWK URI.
     *
     * @param sourceType The type of JWK source (e.g., firebase, x509, generic).
     * @param jwkUri     The URI of the JSON Web Key Set (JWKS).
     * @return An implementation of JwkSource based on the source type.
     */
    public static JwkSource createJwkSource(String sourceType, String jwkUri, Long  cacheTtl) {
        if (sourceType == null || sourceType.isBlank()) {
            // Default to JwkJsonSource if sourceType is null or empty
            return new JwkJsonSource(jwkUri, cacheTtl);
        }

        switch (sourceType.toLowerCase()) {
            case "firebase":
            case "x509":
                return new JwkX509Source(jwkUri, cacheTtl); // X.509 certificate-based JWK source
            case "generic":
                return new JwkJsonSource(jwkUri, cacheTtl); // JSON-based JWK source
            default:
                // Default to JwkJsonSource for any unrecognized type
                return new JwkJsonSource(jwkUri, cacheTtl);
        }
    }
}
