package io.jetproxy.middleware.auth;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.ConfigLoader;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForwardAuthAuthenticatorTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpURLConnection mockConnection;

    @Mock
    private ServletRequest servletRequest;

    @Mock
    private ServletResponse servletResponse;

    private TestForwardAuthAuthenticator authenticator;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        AppConfig.Middleware middleware = new AppConfig.Middleware();
        AppConfig.ForwardAuth forwardAuth = new AppConfig.ForwardAuth();
        forwardAuth.setService("authService");
        forwardAuth.setPath("/validate");
        forwardAuth.setRequestHeaders("Forward(Authorization)");
        forwardAuth.setResponseHeaders("Forward(X-Auth-*)");
        middleware.setForwardAuth(forwardAuth);

        AppConfig.Service service = new AppConfig.Service();
        service.setUrl("http://auth.example.com");
        service.setMethods(List.of("POST"));
        ConfigLoader.loadConfig("config.yaml");

        ConfigLoader.getServiceMap().put("authService", service);

        authenticator = new TestForwardAuthAuthenticator(middleware, mockConnection);
    }

    @Test
    void testValidateRequest_SuccessfulAuthentication() throws Exception {
        Request request = mock(Request.class);

        when(request.getMethod()).thenReturn("GET");


        when(mockResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        // Mock forward auth request
        Map<String, String> mockHeaders = Map.of("Authorization", "Bearer abc123");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(mockHeaders.keySet()));
        when(request.getHeader("Authorization")).thenReturn("Bearer abc123");

        // Mock HTTP Connection response
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getHeaderFields()).thenReturn(Collections.emptyMap());

        Authentication result = authenticator.validateRequest(request, mockResponse, true);

        assertTrue(result instanceof Authentication.User);
    }

    @Test
    void testValidateRequest_AuthenticationFailure() throws Exception {
        Request request = mock(Request.class);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Authorization", List.of("Bearer invalid_token"));

        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));

        when(mockResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
        when(mockConnection.getHeaderFields()).thenReturn(Collections.emptyMap());

        Authentication result = authenticator.validateRequest(request, mockResponse, true);

        assertEquals(Authentication.UNAUTHENTICATED, result);
        verify(mockResponse).sendError(HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized");
    }

    @Test
    void testValidateRequest_IOExceptionHandling() throws Exception {
        Request request = mock(Request.class);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Authorization", List.of("Bearer invalid_token"));

        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(request.getAttribute(Request.class.getName())).thenReturn(request);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
        doThrow(new IOException("Connection failed")).when(mockConnection).connect();
        // Trigger the IOException in the override
        authenticator.setThrowException(true);
        Authentication result = authenticator.validateRequest(request, mockResponse, true);

        assertEquals(Authentication.SEND_FAILURE, result);
        verify(mockResponse).sendError(HttpURLConnection.HTTP_UNAVAILABLE, "Service Unavailable");
    }
}
