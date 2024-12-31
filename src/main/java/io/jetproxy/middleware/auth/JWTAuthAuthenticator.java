package io.jetproxy.middleware.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jetproxy.context.AppContext;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import io.jetproxy.context.AppConfig;

import javax.security.auth.Subject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.stream.Collectors;

public class JWTAuthAuthenticator implements Authenticator {
    public static final String HEADER_NAME = "jetproxy-jwt-claims";
    private final AppConfig.JwtAuthSource jwtAuthSource;
    private final Key secretKey;

    public JWTAuthAuthenticator() {
        this.jwtAuthSource = AppContext.get().getConfig().getJwtAuthSource();

        // Initialize the secret key if configured
            if (jwtAuthSource.getSecretKey() != null) {
                this.secretKey = Keys.hmacShaKeyFor(jwtAuthSource.getSecretKey().getBytes());
            } else {
                this.secretKey = null;
            }
    }

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {}

    @Override
    public String getAuthMethod() {
        return "JWT";
    }

    @Override
    public void prepareRequest(ServletRequest servletRequest) {}

    @Override
    public Authentication validateRequest(ServletRequest servletRequest, ServletResponse servletResponse, boolean mandatory) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            // Extract and validate the token
            String token = extractToken(request);
            if (token == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
                return Authentication.UNAUTHENTICATED;
            }

            Claims claims = validateToken(token);

            // Optional: Validate claims
            if (!validateClaims(claims)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Claim validation failed");
                return Authentication.UNAUTHENTICATED;
            }
            forwardClaimsToHeader(claims, request);
            // Return an authenticated user
            return new UserAuthentication(getAuthMethod(), new JWTUserIdentity(claims));

        } catch (SignatureException | MalformedJwtException e) {
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token signature");
            } catch (Exception ignored) {}
            return Authentication.UNAUTHENTICATED;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication service error");
            } catch (Exception ignored) {}
            return Authentication.UNAUTHENTICATED;
        }
    }

    @Override
    public boolean secureResponse(ServletRequest servletRequest, ServletResponse servletResponse, boolean isSecure, Authentication.User authenticatedUser) {
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String headerValue = request.getHeader(jwtAuthSource.getHeaderName());
        if (headerValue != null && headerValue.startsWith(jwtAuthSource.getTokenPrefix())) {
            return headerValue.substring(jwtAuthSource.getTokenPrefix().length()).trim();
        }
        return null;
    }

    private Claims validateToken(String token) throws Exception {
        if (secretKey != null) {
            // Validate using secretKey (HS256)
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } else if (jwtAuthSource.getJwksUri() != null) {
            // Validate using JWKS (RS256)
            RSAPublicKey publicKey = fetchPublicKeyFromJWKS(token);
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } else {
            throw new SignatureException("No valid key configuration found for JWT validation.");
        }
    }

    private String extractKidFromJwt(String token) throws Exception {
        // Split the JWT into its parts: header.payload.signature
        String[] parts = token.split("\\.");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid JWT format: Must contain header, payload, and signature.");
        }

        // Decode the header (first part of the JWT)
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

        // Parse the header JSON to extract the `kid`
        JsonObject header = JsonParser.parseString(headerJson).getAsJsonObject();
        if (!header.has("kid")) {
            throw new IllegalArgumentException("No 'kid' found in JWT header.");
        }

        return header.get("kid").getAsString();
    }

    private RSAPublicKey fetchPublicKeyFromJWKS(String token) throws Exception {
        // Extract the `kid` from the JWT header
        String kid = extractKidFromJwt(token);

//                (String) Jwts.parserBuilder()
//                .build()
//                .parseClaimsJwt(token)
//                .getHeader()
//                .get("kid");

        if (kid == null) {
            throw new Exception("No 'kid' (Key ID) found in JWT header.");
        }

        // Fetch JWKS from the URI
        URL url = new URL(jwtAuthSource.getJwksUri());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new Exception("Failed to fetch JWKS: HTTP " + connection.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String jwksResponse = reader.lines().collect(Collectors.joining());
        reader.close();

        // Parse the JWKS and extract the public key
        return extractPublicKeyFromJWKS(jwksResponse, kid);
    }



    private RSAPublicKey extractPublicKeyFromJWKS(String jwksResponse, String kid) throws Exception {
        // Parse the JWKS response as JSON
        JsonObject jwksJson = JsonParser.parseString(jwksResponse).getAsJsonObject();
        JsonArray keys = jwksJson.getAsJsonArray("keys");

        for (JsonElement keyElement : keys) {
            JsonObject key = keyElement.getAsJsonObject();
            if (kid.equals(key.get("kid").getAsString())) {
                String modulusBase64 = key.get("n").getAsString();
                String exponentBase64 = key.get("e").getAsString();

                // Construct the RSA public key
                return constructRSAPublicKey(modulusBase64, exponentBase64);
            }
        }

        throw new Exception("No matching key found in JWKS for kid: " + kid);
    }

    private RSAPublicKey constructRSAPublicKey(String modulusBase64, String exponentBase64) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(modulusBase64));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(exponentBase64));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) factory.generatePublic(spec);
    }


    private boolean validateClaims(Claims claims) {
        Map<String, Object> claimValidations = jwtAuthSource.getClaimValidations();

        for (Map.Entry<String, Object> validation : claimValidations.entrySet()) {
            String claimKey = validation.getKey();
            Object expectedValue = validation.getValue();

            if (claims.containsKey(claimKey)) {
                Object actualValue = claims.get(claimKey);
                if (!expectedValue.equals(actualValue)) {
                    return false;
                }
            }
        }
        return true;
    }
    private void forwardClaimsToHeader(Claims claims, HttpServletRequest response) {
        String claimsJson = AppContext.get().gson.toJson(claims); // Convert claims to JSON string
        response.setAttribute(HEADER_NAME, claimsJson);
    }

    private static class JWTUserIdentity implements UserIdentity {
        private final Claims claims;

        public JWTUserIdentity(Claims claims) {
            this.claims = claims;
        }

        @Override
        public Subject getSubject() {
            return new Subject();
        }

        @Override
        public Principal getUserPrincipal() {
            return () -> claims.getSubject(); // Use 'sub' as the user identifier
        }

        @Override
        public boolean isUserInRole(String role, Scope scope) {
            return false; // Role-based validation not implemented
        }
    }
}
