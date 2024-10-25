package proxy.middleware.auth;

import org.eclipse.jetty.security.ConstraintSecurityHandler;

public interface AuthProvider {
    ConstraintSecurityHandler createSecurityHandler(String whitelistPath, String roles);
}
