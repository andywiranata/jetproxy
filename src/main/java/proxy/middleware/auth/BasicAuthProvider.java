package proxy.auth;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.UserStore;
import proxy.context.AppConfig;
import proxy.context.AppContext;

import java.util.List;

public class BasicAuthProvider implements AuthProvider {

    @Override
    public ConstraintSecurityHandler createSecurityHandler(String whitelistPath, String roles) {
        AppConfig config = AppContext.get().getConfig();
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticator(new BasicAuthenticator());

        HashLoginService loginService = new HashLoginService(config.getRealmName());
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

    public ConstraintMapping createConstraintMapping(String pathSpec, String role) {
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{role});
        constraint.setAuthenticate(true);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(pathSpec);

        return mapping;
    }
}
