package io.jetproxy.middleware.auth;

import io.jetproxy.context.AppConfig;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.security.UserStore;

import java.util.List;

public class BasicAuthProvider implements AuthProvider {

    @Override
    public ConstraintSecurityHandler createSecurityHandler(AppConfig config) {
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        HashLoginService loginService = new HashLoginService("");
        UserStore userStore = new UserStore();

        List<AppConfig.User> users = config.getUsers();
        if (users != null) {
            for (AppConfig.User user : users) {
                String username = user.getUsername();
                String password = user.getPassword();
                String role = user.getRole();
                userStore.addUser(username, Credential.getCredential(password), new String[]{role});
            }
        }
        loginService.setUserStore(userStore);
        securityHandler.setLoginService(loginService);
        return securityHandler;
    }

    @Override
    public boolean shouldEnableAuth(AppConfig.Proxy proxy) {
        if (proxy.getMiddleware() == null || proxy.getMiddleware().getBasicAuth() == null) {
            return false;
        }
        String authProviderType = getAuthProviderType(proxy);
        String authRoles = getAuthRoles(proxy);
        return authProviderType.equalsIgnoreCase("basicAuth") && !authRoles.isEmpty();
    }

    @Override
    public String getAuthRoles(AppConfig.Proxy proxy) {
        String[] middlewareParts = getMiddlewareParts(proxy);
        return (middlewareParts.length > 1) ? middlewareParts[1] : "";
    }

    @Override
    public ConstraintMapping createConstraintMapping(String pathSpec, String role) {
        // Create a constraint that requires authentication for a specific role
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{role});
        constraint.setAuthenticate(true);

        // Create a mapping between the constraint and the path
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(pathSpec);

        return mapping;
    }

    private String getAuthProviderType(AppConfig.Proxy proxy) {
        String[] middlewareParts = getMiddlewareParts(proxy);
        return (middlewareParts.length > 0) ? middlewareParts[0] : "";
    }

    private String[] getMiddlewareParts(AppConfig.Proxy proxy) {
        String authMiddleware = proxy.getMiddleware() != null ? proxy.getMiddleware().getBasicAuth() : "";
        return authMiddleware.split(":");
    }
}
