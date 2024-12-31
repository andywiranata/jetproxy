package io.jetproxy.middleware.auth.jwk;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;


public interface JwkSource {
    RSAPublicKey getPublicKey(String kid) throws Exception; // Fetch key for a given `kid`
    Map<String, RSAPublicKey> refreshKeys(String kid) throws Exception; // Refresh the cache
}
