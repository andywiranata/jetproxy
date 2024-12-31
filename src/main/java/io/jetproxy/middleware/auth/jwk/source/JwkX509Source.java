package io.jetproxy.middleware.auth.jwk.source;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JwkX509Source extends BaseJwkSource {

    public JwkX509Source(String jwksUri, Long cacheTtl) {
        super(jwksUri, cacheTtl);
    }

    @Override
    protected Map<String, RSAPublicKey> parseJwks(String jwksResponse) throws Exception {
        JsonObject jwksJson = JsonParser.parseString(jwksResponse).getAsJsonObject();
        Map<String, RSAPublicKey> keys = new HashMap<>();
        for (var entry : jwksJson.entrySet()) {
            String kid = entry.getKey();
            String certificate = entry.getValue().getAsString();
            RSAPublicKey publicKey = convertCertificateToPublicKey(certificate);
            keys.put(kid, publicKey);
        }
        return keys;
    }

    private RSAPublicKey convertCertificateToPublicKey(String certificate) throws Exception {
        String formattedCert = certificate.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] certBytes = Base64.getDecoder().decode(formattedCert);

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        var x509Cert = (java.security.cert.X509Certificate)
                factory.generateCertificate(new java.io.ByteArrayInputStream(certBytes));

        return (RSAPublicKey) x509Cert.getPublicKey();
    }

}