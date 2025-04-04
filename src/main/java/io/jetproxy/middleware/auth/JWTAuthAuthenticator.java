package io.jetproxy.middleware.auth;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.auth.jwk.validator.JwtValidator;
import io.jsonwebtoken.*;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import io.jsonwebtoken.security.Keys;
import io.jetproxy.context.AppConfig;

import javax.security.auth.Subject;
import java.security.Key;
import java.security.Principal;
import java.util.*;

import static io.jetproxy.util.Constants.REQUEST_ATTRIBUTE_JETPROXY_JWT_CLAIMS;

public class JWTAuthAuthenticator implements Authenticator {
    @Getter
    private final AppConfig.JwtAuthSource jwtAuthSource;
    @Getter
    private final Key secretKey;
    @Getter
    private JwtValidator jwtValidator;

    public JWTAuthAuthenticator() {
        this.jwtAuthSource = AppContext.get().getConfig().getJwtAuthSource();

        // Initialize the secret key if configured
            if (jwtAuthSource.getSecretKey() != null) {
                this.secretKey = Keys.hmacShaKeyFor(jwtAuthSource.getSecretKey().getBytes());
                this.jwtValidator =  new JwtValidator(
                        jwtAuthSource.getJwksType(),
                        jwtAuthSource.getJwksUri(),
                        jwtAuthSource.getJwksTtl()
                );
            } else {
                this.secretKey = null;
                this.jwtValidator = new JwtValidator(
                        jwtAuthSource.getJwksType(),
                        jwtAuthSource.getJwksUri(),
                        jwtAuthSource.getJwksTtl()
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
            return this.jwtValidator
                    .validateTokenWithSigningKey(this.secretKey, token);
        } else if (jwtAuthSource.getJwksUri() != null) {
            // Validate using JWKS (RS256)
            return this.jwtValidator
                    .validateTokenWithPublicKey(token);
        } else {
            throw new SignatureException("No valid key configuration found for JWT validation.");
        }
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
        String claimsJson = AppContext.get().getGson().toJson(claims); // Convert claims to JSON string
        response.setAttribute(REQUEST_ATTRIBUTE_JETPROXY_JWT_CLAIMS, claimsJson);
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
