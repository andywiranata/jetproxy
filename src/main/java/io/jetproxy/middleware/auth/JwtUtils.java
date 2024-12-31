package io.jetproxy.middleware.auth;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jose.crypto.MACVerifier;

public class JwtUtils {

    public static JWTClaimsSet parseToken(String token, String secretKey) throws Exception {
        // Parse the JWT
        JWSObject jwsObject = JWSObject.parse(token);

        // Verify the signature using the secret key
        if (!jwsObject.verify(new MACVerifier(secretKey))) {
            throw new SecurityException("Invalid signature");
        }

        // Extract the claims set
        return JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
    }
}
