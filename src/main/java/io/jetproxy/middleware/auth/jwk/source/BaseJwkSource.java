package io.jetproxy.middleware.auth.jwk.source;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.auth.jwk.JwkSource;
import io.jetproxy.middleware.cache.Cache;
import io.jetproxy.middleware.cache.CacheFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.jetproxy.util.RequestUtils.parseMaxAge;

public class BaseJwkSource implements JwkSource {

    protected final String jwksUri;
    protected Cache cache;
    protected long cacheTtl = 300;

    protected BaseJwkSource(String jwksUri, long cacheTtl) {
        this.jwksUri = jwksUri;
        this.cache = AppContext.get().getCache();
        this.cacheTtl = cacheTtl;
    }

    @Override
    public RSAPublicKey getPublicKey(String kid) throws Exception {
        String jwksResponse = this.cache.get(String.format(CacheFactory.HTTP_JWT_AUTH_SOURCE_CACHE_KEY, AppContext.get().getInstanceId(), kid));
        if (jwksResponse != null) {
            return parseJwks(jwksResponse).get(kid);
        }
        RSAPublicKey publicKey = refreshKeys(kid).get(kid);
        if (publicKey == null) {
            throw new IllegalArgumentException("No matching key found for kid: " + kid);
        }

        return publicKey;
    }

    @Override
    public Map<String, RSAPublicKey> refreshKeys(String kid) throws Exception {
        String jwksResponse = fetchJwks();
        this.cache.put(String.format(
                CacheFactory.HTTP_JWT_AUTH_SOURCE_CACHE_KEY, AppContext.get().getInstanceId(), kid),
                jwksResponse, cacheTtl);
        return parseJwks(jwksResponse);
    }

    private String fetchJwks() throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(jwksUri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                throw new IllegalStateException("Failed to fetch JWKS: HTTP " + connection.getResponseCode());
            }

            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
                return reader.lines().collect(Collectors.joining());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    /**
     * Subclasses should implement this method to parse the JWKS response and populate the key cache.
     *
     * @param jwksResponse JSON response containing JWKS.
     * @throws Exception if parsing fails.
     */
    protected Map<String, RSAPublicKey> parseJwks(String jwksResponse) throws Exception {
        throw new UnsupportedOperationException("The method parseJwks is not implemented yet. Please override this method in a subclass.");
    }

}