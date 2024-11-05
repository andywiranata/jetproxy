package proxy.middleware.auth;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import proxy.context.AppConfig;

public interface AuthProvider {
    ConstraintSecurityHandler createSecurityHandler(AppConfig config);

    // Checks if this auth provider applies to the given proxy rule
    boolean shouldEnableAuth(AppConfig.Proxy proxy);

    // Retrieves roles if auth is required for the given proxy
    String getAuthRoles(AppConfig.Proxy proxy);
}

