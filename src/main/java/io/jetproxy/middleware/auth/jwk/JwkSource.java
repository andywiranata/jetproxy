package io.jetproxy.middleware.auth.jwk;

import java.security.interfaces.RSAPublicKey;


public interface JwkSource {
    RSAPublicKey getPublicKey(String kid) throws Exception; // Fetch key for a given `kid`
    void refreshKeys() throws Exception; // Refresh the cache
}
