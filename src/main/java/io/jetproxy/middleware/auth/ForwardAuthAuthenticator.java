package io.jetproxy.middleware.auth;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.CacheFactory;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.ConfigLoader;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.rule.header.HeaderAction;
import io.jetproxy.middleware.rule.header.HeaderActionFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

public class ForwardAuthAuthenticator implements Authenticator {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ForwardAuthAuthenticator.class);
    private final String path;
    private final List<HeaderAction> requestHeaderActions;
    private final List<HeaderAction> responseHeaderActions; // Actions to handle headers
    private final AppConfig.Service service;
    private final String requestMethod;

    public ForwardAuthAuthenticator(AppConfig.Middleware appMiddleware) {
        assert appMiddleware.getForwardAuth() != null;

        String serviceName = appMiddleware.getForwardAuth().getService();
        String authRequestHeaderRules = appMiddleware.getForwardAuth().getRequestHeaders();
        String authResponseHeaderRules = appMiddleware.getForwardAuth().getResponseHeaders();

        this.service = ConfigLoader.getServiceMap().get(serviceName);
        this.path = appMiddleware.getForwardAuth().getPath();
        this.requestHeaderActions = HeaderActionFactory.createActions(authRequestHeaderRules);
        this.responseHeaderActions = HeaderActionFactory.createActions(authResponseHeaderRules);
        this.requestMethod = this.service.getMethods().getFirst();
    }

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {}

    @Override
    public String getAuthMethod() {
        return "forwardAuth";
    }

    @Override
    public void prepareRequest(ServletRequest servletRequest) {
    }

    @Override
    public Authentication validateRequest(ServletRequest servletRequest,
                                          ServletResponse servletResponse, boolean mandatory) {
        Request request = Request.getBaseRequest(servletRequest);
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        long startTime = System.currentTimeMillis();
        String authUrl = service.getUrl() + path;
        String responseStatus = "";
        Map<String, String> forwardHeaders;
        Map<String, String> forwardResponseHeaders;

        HttpURLConnection connection;
        int responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR; // Default in case of an exception

        try {

            forwardHeaders = getRequestForwardHeaders(request);
            connection = performForwardAuthRequest(authUrl, forwardHeaders);
            forwardResponseHeaders = getResponseForwardHeaders(connection);
            responseCode = connection.getResponseCode();

            for (Map.Entry<String, String> entry : forwardResponseHeaders.entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return new UserAuthentication(getAuthMethod(), new MockUserIdentity());
            } else {
                responseStatus = "Unauthorized";
                response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, responseStatus);
                return Authentication.UNAUTHENTICATED;
            }
        } catch (IOException e) {
            logger.error("ForwardAuth failed: {} code: {}", authUrl, responseCode);
            try {
                responseStatus = "Service Unavailable";
                response.sendError(HttpURLConnection.HTTP_UNAVAILABLE, responseStatus);
            } catch (IOException ignored) {
                logger.error("Failed to send error response: {}", ignored.getMessage());
            }
            return Authentication.SEND_FAILURE;
        }
    }


    @Override
    public boolean secureResponse(ServletRequest servletRequest, ServletResponse servletResponse, boolean b, Authentication.User user) throws ServerAuthException {
        return true;
    }

    private Map<String, String> getRequestForwardHeaders(HttpServletRequest request) {
        Map<String, String> headersToForward = new HashMap<>();
        // Execute all header actions to populate headersToForward
        for (HeaderAction action : requestHeaderActions) {
            action.execute(request, headersToForward);
        }

        return headersToForward;
    }
    private Map<String, String> getResponseForwardHeaders(HttpURLConnection responseAuthConnection) {
        Map<String, String> headersRequest = responseAuthConnection.getHeaderFields()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null) // Exclude null header names
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(",", entry.getValue()) // Join multiple values with a comma
                ));

        Map<String, String> headersToForward = new HashMap<>();
        responseHeaderActions.forEach(action -> action.execute(headersRequest, headersToForward));

        return headersToForward;
    }


    protected HttpURLConnection performForwardAuthRequest(String authUrl, Map<String, String> headers) throws IOException {
        URL url = new URL(authUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(this.requestMethod);
        connection.setDoOutput(true);

        // Set headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        connection.connect();
        return connection;
    }

    // Inner class to represent a mock authenticated user
    private static class MockUserIdentity implements UserIdentity {
        @Override
        public Subject getSubject() {
            return new Subject();
        }

        @Override
        public Principal getUserPrincipal() {
            return () -> "mockUser";
        }

        @Override
        public boolean isUserInRole(String role, Scope scope) {
            return true; // Allow all roles in this mock
        }
    }
}
