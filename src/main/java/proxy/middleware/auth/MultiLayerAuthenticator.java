package proxy.middleware.auth;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;

import java.util.ArrayList;
import java.util.List;

public class MultiLayerAuthenticator implements Authenticator {
    private final List<Authenticator> authenticators = new ArrayList<>();

    public void addAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        for (Authenticator authenticator : authenticators) {
            authenticator.setConfiguration(configuration);
        }
    }

    @Override
    public String getAuthMethod() {
        return "MULTI_LAYER";
    }

    @Override
    public void prepareRequest(ServletRequest request) {
        for (Authenticator authenticator : authenticators) {
            authenticator.prepareRequest(request);
        }
    }

    @Override
    public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory) throws ServerAuthException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Try each authenticator in sequence
        for (Authenticator authenticator : authenticators) {
            Authentication authentication = authenticator.validateRequest(httpRequest, httpResponse, mandatory);

            if (authentication instanceof Authentication.User) {
                // Authentication succeeded with this authenticator
                return authentication;
            }
        }

        // If none of the authenticators succeeded, return NOT_AUTHENTICATED
        return Authentication.UNAUTHENTICATED;
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        for (Authenticator authenticator : authenticators) {
            if (authenticator.secureResponse(request, response, mandatory, validatedUser)) {
                return true;
            }
        }
        return false;
    }
}
