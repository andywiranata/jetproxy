package io.jetproxy.middleware.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.auth.jwk.validator.BaseJwtValidator;
import io.jetproxy.middleware.auth.jwk.validator.JwtValidator;
import io.jsonwebtoken.*;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
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
    private JwtValidator jwtValidator;

    public JWTAuthAuthenticator() {
        this.jwtAuthSource = AppContext.get().getConfig().getJwtAuthSource();

        // Initialize the secret key if configured
            if (jwtAuthSource.getSecretKey() != null) {
                this.secretKey = Keys.hmacShaKeyFor(jwtAuthSource.getSecretKey().getBytes());
            } else {
                this.secretKey = null;
                this.jwtValidator = new JwtValidator(
                        jwtAuthSource.getJwksType(),
                        jwtAuthSource.getJwksUri()
                );

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
        } catch (ExpiredJwtException e) {
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
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
            return this.jwtValidator.validateToken(token);
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
