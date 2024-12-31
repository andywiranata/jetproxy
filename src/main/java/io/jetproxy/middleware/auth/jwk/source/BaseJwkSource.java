package io.jetproxy.middleware.auth.jwk.source;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.auth.jwk.JwkSource;
import io.jetproxy.middleware.cache.Cache;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public class BaseJwkSource implements JwkSource {

    private final String jwksUri;
    protected final Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();
    protected Cache cache;

    protected BaseJwkSource(String jwksUri) {
        this.jwksUri = jwksUri;
        this.cache = AppContext.get().getCache();
    }

    @Override
    public RSAPublicKey getPublicKey(String kid) throws Exception {
        if (keyCache.containsKey(kid)) {
            return keyCache.get(kid);
        }

        refreshKeys();

        RSAPublicKey publicKey = keyCache.get(kid);
        if (publicKey == null) {
            throw new IllegalArgumentException("No matching key found for kid: " + kid);
        }

        return publicKey;
    }

    @Override
    public void refreshKeys() throws Exception {
        String jwksResponse = fetchJwks();
        parseJwks(jwksResponse);
    }

    private String fetchJwks() throws Exception {
        URL url = new URL(jwksUri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new IllegalStateException("Failed to fetch JWKS: HTTP " + connection.getResponseCode());
        }

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    /**
     * Subclasses should implement this method to parse the JWKS response and populate the key cache.
     *
     * @param jwksResponse JSON response containing JWKS.
     * @throws Exception if parsing fails.
     */
    protected void parseJwks(String jwksResponse) throws Exception {

    }
    protected String serializePublicKey(RSAPublicKey publicKey) {
        return null;
    }
    protected RSAPublicKey deserializePublicKey(String serializedKey)  throws Exception {
        return null;
    }
}