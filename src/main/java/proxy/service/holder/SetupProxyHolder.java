package proxy.service.holder;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;
import proxy.context.AppContext;
import proxy.context.ConfigLoader;

import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;

import java.util.List;

public class SetupProxyHolder {
    private static final String PROXY_TO = "proxyTo";
    private static final String PREFIX = "prefix";
    private static final String TIMEOUT = "timeout";
    private static final Logger logger = LoggerFactory.getLogger(SetupProxyHolder.class);
    private final AppConfig config;

    public SetupProxyHolder(AppConfig config) {
        this.config = config;
    }

    public void setupProxies(Server server, ServletContextHandler context) {
        List<AppConfig.Proxy> proxies = config.getProxies();
        ConstraintSecurityHandler securityHandler = createBasicAuthSecurityHandler();
        for (AppConfig.Proxy proxyRule : proxies) {
            AppConfig.Service service = ConfigLoader.getServiceMap().get(proxyRule.getService());
            String targetServiceUrl = service.getUrl();
            String whitelistPath = proxyRule.getPath() + "/*";
            String middleware = proxyRule.getMiddleware();
            String[] middlewareParts = middleware.split(":");
            // Extract "basicAuth" and "roleA"
            String authProvider = (middlewareParts.length > 0 && middlewareParts[0] != null) ? middlewareParts[0] : "";
            String authRoles = (middlewareParts.length > 1 && middlewareParts[1] != null) ? middlewareParts[1] : "";

            if (targetServiceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
            }
            ServletHolder proxyServlet = new ServletHolder(new ProxyHolder(service, proxyRule));
            proxyServlet.setInitParameter(PROXY_TO, targetServiceUrl);
            proxyServlet.setInitParameter(PREFIX, proxyRule.getPath());
            proxyServlet.setInitParameter(TIMEOUT, String.valueOf(config.getDefaultTimeout()));

            if (authProvider.equalsIgnoreCase("basicAuth")) {
                if (!authRoles.isEmpty()) {
                    securityHandler.addConstraintMapping(
                            createConstraintMapping(whitelistPath, authRoles));

                }
            }
            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            securityHandler.setHandler(context);
            server.setHandler(context);
            server.setHandler(securityHandler);

            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);
        }
    }

    protected ConstraintSecurityHandler createBasicAuthSecurityHandler() {
        AppConfig config = AppContext.get().getConfig();
        // Create and configure the security handler
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticator(new BasicAuthenticator());

        // Use a PropertyFileLoginModule and point to the realm.properties
        HashLoginService loginService = new HashLoginService(config.getRealmName());

        UserStore userStore = new UserStore(); // Use UserStore to manage users

        List<AppConfig.User> users = config.getUsers();
        for (AppConfig.User user : users) {
            String username = user.getUsername();
            String password = user.getPassword();
            String role = user.getRole();
            // Add users and roles programmatically to UserStore
            userStore.addUser(username, Credential.getCredential(password), new String[]{role});
        }

        loginService.setUserStore(userStore);
        // Define constraint for /product for userA (roleA)
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }

    protected ConstraintMapping createConstraintMapping(String pathSpec, String role) {
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
}
