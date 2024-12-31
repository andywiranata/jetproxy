package io.jetproxy.middleware.auth.jwk.source;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class JwkX509Source extends BaseJwkSource {

    public JwkX509Source(String jwksUri) {
        super(jwksUri);
    }

    @Override
    protected void parseJwks(String jwksResponse) throws Exception {
        JsonObject jwksJson = JsonParser.parseString(jwksResponse).getAsJsonObject();
        for (var entry : jwksJson.entrySet()) {
            String kid = entry.getKey();
            String certificate = entry.getValue().getAsString();
            RSAPublicKey publicKey = convertCertificateToPublicKey(certificate);
            keyCache.put(kid, publicKey);
        }
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

    @Override
    protected String serializePublicKey(RSAPublicKey publicKey) {
        BigInteger modulus = publicKey.getModulus();
        BigInteger exponent = publicKey.getPublicExponent();

        return Base64.getEncoder().encodeToString(modulus.toByteArray()) + ":" +
                Base64.getEncoder().encodeToString(exponent.toByteArray());
    }


    @Override
    protected RSAPublicKey deserializePublicKey(String serializedKey) throws Exception {
        String[] parts = serializedKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid serialized key format");
        }

        BigInteger modulus = new BigInteger(1, Base64.getDecoder().decode(parts[0]));
        BigInteger exponent = new BigInteger(1, Base64.getDecoder().decode(parts[1]));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
}