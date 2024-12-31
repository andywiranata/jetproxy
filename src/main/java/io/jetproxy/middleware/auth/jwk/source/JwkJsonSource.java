package io.jetproxy.middleware.auth.jwk.source;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;


public class JwkJsonSource extends BaseJwkSource {

    public JwkJsonSource(String jwksUri) {
        super(jwksUri);
    }

    @Override
    protected void parseJwks(String jwksResponse) throws Exception {
        JsonObject jwksJson = JsonParser.parseString(jwksResponse).getAsJsonObject();
        JsonArray keys = jwksJson.getAsJsonArray("keys");

        for (JsonElement keyElement : keys) {
            JsonObject key = keyElement.getAsJsonObject();
            String kid = key.get("kid").getAsString();
            String modulusBase64 = key.get("n").getAsString();
            String exponentBase64 = key.get("e").getAsString();

            RSAPublicKey publicKey = constructRSAPublicKey(modulusBase64, exponentBase64);
            keyCache.put(kid, publicKey);
        }
    }

    private RSAPublicKey constructRSAPublicKey(String modulusBase64, String exponentBase64) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(modulusBase64));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(exponentBase64));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) factory.generatePublic(spec);
    }

    protected String serializePublicKey(RSAPublicKey publicKey) {
        BigInteger modulus = publicKey.getModulus();
        BigInteger exponent = publicKey.getPublicExponent();
        return Base64.getEncoder().encodeToString(modulus.toByteArray()) + ":" +
                Base64.getEncoder().encodeToString(exponent.toByteArray());
    }

    protected RSAPublicKey deserializePublicKey(String serializedKey) throws Exception {
        String[] parts = serializedKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid serialized key format");
        }

        BigInteger modulus = new BigInteger(1, Base64.getDecoder().decode(parts[0]));
        BigInteger exponent = new BigInteger(1, Base64.getDecoder().decode(parts[1]));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(spec);
    }

}

