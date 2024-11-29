package proxy.middleware.auth;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import proxy.context.AppConfig;
import proxy.context.ConfigLoader;
import proxy.logger.DebugAwareLogger;
import proxy.middleware.rule.header.HeaderAction;
import proxy.middleware.rule.header.HeaderActionFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.util.*;

public class ForwardAuthAuthenticator implements Authenticator {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ForwardAuthAuthenticator.class);

    private final String path;
    private final List<HeaderAction> headerActions; // Actions to handle headers
    private final AppConfig.Service service;
    private final String requestMethod;

    public ForwardAuthAuthenticator(AppConfig.Middleware appMiddleware) {
        assert appMiddleware.getForwardAuth() != null;

        String serviceName = appMiddleware.getForwardAuth().getService();
        String authRequestHeaderRules = appMiddleware.getForwardAuth().getAuthRequestHeaders();

        this.service = ConfigLoader.getServiceMap().get(serviceName);
        this.path = appMiddleware.getForwardAuth().getPath();
        this.headerActions = HeaderActionFactory.createActions(authRequestHeaderRules);
        this.requestMethod = this.service.getMethods().getFirst();
    }

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {
        logger.debug("setConfiguration");
    }

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
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            // Extract headers to forward
            Map<String, String> forwardHeaders = getForwardHeaders(request);

            // Perform the forward authentication request
            HttpURLConnection connection = performForwardAuthRequest(forwardHeaders);

            // Validate the response
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return new UserAuthentication(getAuthMethod(), new MockUserIdentity());
            } else {
                response.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized");
                return Authentication.UNAUTHENTICATED;
            }
        } catch (IOException e) {
            logger.error("Error during authentication", e);
            try {
                response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
            } catch (IOException ignored) {
            }
            return Authentication.UNAUTHENTICATED;
        }
    }

    @Override
    public boolean secureResponse(ServletRequest servletRequest, ServletResponse servletResponse, boolean b, Authentication.User user) throws ServerAuthException {
        return true;
    }

    private Map<String, String> getForwardHeaders(HttpServletRequest request) {
        Map<String, String> headersToForward = new HashMap<>();
        // Execute all header actions to populate headersToForward
        for (HeaderAction action : headerActions) {
            action.execute(request, headersToForward);
        }

        return headersToForward;
    }

    private HttpURLConnection performForwardAuthRequest(Map<String, String> headers) throws IOException {
        URL url = new URL(service.getUrl() + path);
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
