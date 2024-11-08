package proxy.middleware.auth;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.security.Constraint;
import proxy.context.AppConfig;

@Deprecated
public class ForwardAuthProvider implements AuthProvider {
    @Override
    public ConstraintSecurityHandler createSecurityHandler(AppConfig config) {
        ConstraintSecurityHandler securityHandlerForwardAuth = new ConstraintSecurityHandler();
        securityHandlerForwardAuth.setAuthenticator(new ForwardAuthAuthenticator());
        return securityHandlerForwardAuth;
    }

    @Override
    public boolean shouldEnableAuth(AppConfig.Proxy proxy) {
        return proxy.getMiddleware() != null && proxy.getMiddleware().hasForwardAuth();
    }

    @Override
    public String getAuthRoles(AppConfig.Proxy proxy) {
        return null;
    }

    @Override
    public ConstraintMapping createConstraintMapping(String pathSpec, String role) {
        Constraint constraint = new Constraint();
        constraint.setName("externalToken");
        constraint.setAuthenticate(true); // Set authenticate to true
        constraint.setRoles(new String[]{"user", "admin"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec(pathSpec);
        mapping.setConstraint(constraint);

        return mapping;
    }
}
