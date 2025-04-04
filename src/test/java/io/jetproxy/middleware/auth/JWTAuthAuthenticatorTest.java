package io.jetproxy.middleware.auth;


import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.jetproxy.util.Constants.REQUEST_ATTRIBUTE_JETPROXY_JWT_CLAIMS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JWTAuthAuthenticatorTest {

    private JWTAuthAuthenticator authenticator;
    private AppConfig.JwtAuthSource jwtAuthSource;
    private Key secretKey;

    private final String HEADER_NAME = "Authorization";
    private final String PREFIX = "Bearer ";

    @BeforeEach
    void setup() {
        // Mock AppConfig
        jwtAuthSource = new AppConfig.JwtAuthSource();
        jwtAuthSource.setHeaderName(HEADER_NAME);
        jwtAuthSource.setTokenPrefix(PREFIX);
        jwtAuthSource.setSecretKey("mysecretkeymysecretkeymysecretkey12"); // Must be >= 32 bytes

        jwtAuthSource.setClaimValidations(Map.of("role", "admin"));

        AppConfig mockConfig = mock(AppConfig.class);
        when(mockConfig.getJwtAuthSource()).thenReturn(jwtAuthSource);

        // Mock AppContext
        AppContext appContext = mock(AppContext.class);
        when(appContext.getConfig()).thenReturn(mockConfig);
        // when(appContext.gson).thenReturn(new com.google.gson.Gson());

        try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
            mockedAppContext.when(AppContext::get).thenReturn(appContext);

            // Instantiate authenticator with static mocked AppContext
            authenticator = new JWTAuthAuthenticator();
        }

        // Extract actual signing key
        this.secretKey = authenticator != null ? authenticator.getSecretKey() : null;
    }

    private String generateJwt(Map<String, Object> claims, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + ttlMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("testuser")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    @Test
    void testValidJwtAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Generate a sample JWT
        String jwt = generateJwt(Map.of("role", "admin"), 60000);
        when(request.getHeader(HEADER_NAME)).thenReturn(PREFIX + jwt);

        // Mock AppContext.get().gson.toJson(...)
        try (MockedStatic<AppContext> appContextMock = mockStatic(AppContext.class)) {
            AppContext mockAppContext = mock(AppContext.class);
            Gson mockGson = mock(Gson.class);

            appContextMock.when(AppContext::get).thenReturn(mockAppContext);
            when(mockAppContext.getGson()).thenReturn(mockGson);
            when(mockAppContext.getGson().toJson(any(Claims.class))).thenReturn("{\"role\":\"admin\"}");

            // Run authentication
            Authentication auth = authenticator.validateRequest(request, response, true);

            // Assertions
            assertNotNull(auth);
            assertTrue(auth instanceof Authentication.User);
            verify(request).setAttribute(eq(REQUEST_ATTRIBUTE_JETPROXY_JWT_CLAIMS), eq("{\"role\":\"admin\"}"));
        }
    }
    @Test
    void testMissingToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getHeader(HEADER_NAME)).thenReturn(null);

        Authentication auth = authenticator.validateRequest(request, response, true);

        assertEquals(Authentication.UNAUTHENTICATED, auth);
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("Missing"));
    }

    @Test
    void testInvalidSignature() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Create token with wrong key
        Key wrongKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor("invalidkeyinvalidkeyinvalidkey11".getBytes());
        String jwt = Jwts.builder()
                .setSubject("testuser")
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        when(request.getHeader(HEADER_NAME)).thenReturn(PREFIX + jwt);

        Authentication auth = authenticator.validateRequest(request, response, true);

        assertEquals(Authentication.UNAUTHENTICATED, auth);
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("Invalid"));
    }

    @Test
    void testExpiredToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String jwt = generateJwt(Map.of("role", "admin"), -1000); // already expired
        when(request.getHeader(HEADER_NAME)).thenReturn(PREFIX + jwt);

        Authentication auth = authenticator.validateRequest(request, response, true);

        assertEquals(Authentication.UNAUTHENTICATED, auth);
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), contains("expired"));
    }

    @Test
    void testClaimValidationFailure() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String jwt = generateJwt(Map.of("role", "user"), 60000); // wrong role
        when(request.getHeader(HEADER_NAME)).thenReturn(PREFIX + jwt);

        Authentication auth = authenticator.validateRequest(request, response, true);

        assertEquals(Authentication.UNAUTHENTICATED, auth);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains("Claim validation"));
    }

}