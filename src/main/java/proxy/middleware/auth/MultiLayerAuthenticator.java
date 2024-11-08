package proxy.middleware.auth;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiLayerAuthenticator implements Authenticator {
    private final Map<String, List<Authenticator>> pathAuthenticatorMap = new HashMap<>();

    // Method to add a list of authenticators for a specific path pattern
    public void registerAuthenticators(String pathPattern, List<Authenticator> authenticators) {
        pathAuthenticatorMap.put(pathPattern, authenticators);
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        for (List<Authenticator> authenticators : pathAuthenticatorMap.values()) {
            for (Authenticator authenticator : authenticators) {
                authenticator.setConfiguration(configuration);
            }
        }
    }

    @Override
    public String getAuthMethod() {
        return "FLEXIBLE_PATH";
    }

    @Override
    public void prepareRequest(ServletRequest request) {
        String path = ((HttpServletRequest) request).getPathInfo();
        List<Authenticator> authenticators = getAuthenticatorsForPath(path);

        for (Authenticator authenticator : authenticators) {
            authenticator.prepareRequest(request);
        }
    }

    @Override
    public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory) throws ServerAuthException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getPathInfo();

        List<Authenticator> authenticators = getAuthenticatorsForPath(path);

        // If no authenticators are assigned to the path, return NOT_CHECKED (public path)
        if (authenticators.isEmpty()) {
            return Authentication.NOT_CHECKED;
        }

        // Sequentially try each authenticator until one succeeds
        for (Authenticator authenticator : authenticators) {
            Authentication authentication = authenticator.validateRequest(httpRequest, httpResponse, mandatory);
            if (authentication instanceof Authentication.User) {
                // Return on the first successful authentication
                return authentication;
            }
        }

        // If all authenticators fail, return UNAUTHENTICATED
        return Authentication.UNAUTHENTICATED;
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        String path = ((HttpServletRequest) request).getPathInfo();
        List<Authenticator> authenticators = getAuthenticatorsForPath(path);

        for (Authenticator authenticator : authenticators) {
            if (authenticator.secureResponse(request, response, mandatory, validatedUser)) {
                return true;
            }
        }
        return false;
    }

    // Method to retrieve the list of authenticators for a given path
    private List<Authenticator> getAuthenticatorsForPath(String path) {
        return pathAuthenticatorMap.entrySet().stream()
                .filter(entry -> path.matches(entry.getKey())) // Match path to pattern
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(new ArrayList<>());
    }
}
