package proxy.middleware.auth;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.security.UserStore;
import proxy.context.AppConfig;

import java.util.List;

public class BasicAuthProvider implements AuthProvider {

    @Override
    public ConstraintSecurityHandler createSecurityHandler(AppConfig config) {
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticator(new BasicAuthenticator());

        HashLoginService loginService = new HashLoginService("");
        UserStore userStore = new UserStore();

        List<AppConfig.User> users = config.getUsers();
        for (AppConfig.User user : users) {
            String username = user.getUsername();
            String password = user.getPassword();
            String role = user.getRole();
            userStore.addUser(username, Credential.getCredential(password), new String[]{role});
        }

        loginService.setUserStore(userStore);
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }

    @Override
    public boolean shouldEnableAuth(AppConfig.Proxy proxy) {
        String authProviderType = getAuthProviderType(proxy);
        String authRoles = getAuthRoles(proxy);
        return authProviderType.equalsIgnoreCase("basicAuth") && !authRoles.isEmpty();
    }

    @Override
    public String getAuthRoles(AppConfig.Proxy proxy) {
        String[] middlewareParts = getMiddlewareParts(proxy);
        return (middlewareParts.length > 1) ? middlewareParts[1] : "";
    }

    private String getAuthProviderType(AppConfig.Proxy proxy) {
        String[] middlewareParts = getMiddlewareParts(proxy);
        return (middlewareParts.length > 0) ? middlewareParts[0] : "";
    }

    private String[] getMiddlewareParts(AppConfig.Proxy proxy) {
        String authMiddleware = proxy.getMiddleware() != null ? proxy.getMiddleware().getAuth() : "";
        return authMiddleware.split(":");
    }
}
