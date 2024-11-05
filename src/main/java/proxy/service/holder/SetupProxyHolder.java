package proxy.service.holder;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
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
import proxy.middleware.auth.AuthProvider;
import proxy.middleware.auth.AuthProviderFactory;

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
        AuthProvider authProvider = AuthProviderFactory.getAuthProvider("basicAuth");
        ConstraintSecurityHandler securityHandler = authProvider.createSecurityHandler(this.config);

        for (AppConfig.Proxy proxyRule : proxies) {
            AppConfig.Service service = ConfigLoader.getServiceMap().get(proxyRule.getService());
            String targetServiceUrl = service.getUrl();
            String whitelistPath = proxyRule.getPath() + "/*";

            if (targetServiceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
            }
            ServletHolder proxyServlet = new ServletHolder(new ProxyHolder(service, proxyRule));
            proxyServlet.setInitParameter(PROXY_TO, targetServiceUrl);
            proxyServlet.setInitParameter(PREFIX, proxyRule.getPath());
            proxyServlet.setInitParameter(TIMEOUT, String.valueOf(config.getDefaultTimeout()));

            if (authProvider.shouldEnableAuth(proxyRule)) {
                securityHandler.addConstraintMapping(
                        createConstraintMapping(whitelistPath
                                , authProvider.getAuthRoles(proxyRule)));

            }
            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            securityHandler.setHandler(context);
            server.setHandler(context);
            server.setHandler(securityHandler);

            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);
        }
        FilterHolder metricsFilterHolder = new FilterHolder(new MetricFilter());
        context.addFilter(metricsFilterHolder, "/*", null);
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
