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

import javax.security.auth.Subject;
import java.security.Principal;

public class ForwardAuthAuthenticator implements Authenticator {

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {
        System.out.println("setConfiguration");
    }

    @Override
    public String getAuthMethod() {
        return "forwardAuth";
    }

    @Override
    public void prepareRequest(ServletRequest servletRequest) {
        System.out.println("prepareRequest");

    }

    @Override
    public Authentication validateRequest(ServletRequest servletRequest,
                                          ServletResponse servletResponse, boolean mandatory) {
        System.out.println("validateRequest called");

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Simulate successful token validation by always returning an authenticated user
        return new UserAuthentication(getAuthMethod(), new MockUserIdentity());
    }

    @Override
    public boolean secureResponse(ServletRequest servletRequest, ServletResponse servletResponse, boolean b, Authentication.User user) throws ServerAuthException {
        System.out.println("secureResponse");
        return false;
    }
    // Inner class to represent a mock authenticated user
    private static class MockUserIdentity implements UserIdentity {
        @Override
        public Subject getSubject() {
            return new Subject(); // Empty subject for this mock implementation
        }

        @Override
        public Principal getUserPrincipal() {
            return () -> "mockUser"; // Mock user name
        }

        @Override
        public boolean isUserInRole(String role, Scope scope) {
            return true; // Allow all roles in this mock
        }
    }
}
