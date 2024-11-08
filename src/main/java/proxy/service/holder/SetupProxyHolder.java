package proxy.service.holder;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;
import proxy.context.ConfigLoader;

import proxy.middleware.auth.AuthProviderFactory;
import proxy.middleware.auth.BasicAuthProvider;
import proxy.middleware.auth.ForwardAuthAuthenticator;
import proxy.middleware.auth.MultiLayerAuthenticator;

import java.util.ArrayList;
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
        MultiLayerAuthenticator multiLayerAuthenticator = new MultiLayerAuthenticator();

        BasicAuthProvider basicAuthProvider = (BasicAuthProvider) AuthProviderFactory.getAuthProvider("basicAuth");
        ConstraintSecurityHandler basicAuthSecurityHandler = basicAuthProvider.createSecurityHandler(this.config);

        for (AppConfig.Proxy proxyRule : proxies) {
            AppConfig.Service service = ConfigLoader.getServiceMap().get(proxyRule.getService());
            String targetServiceUrl = service.getUrl();
            List<Authenticator> authenticators = new ArrayList<>();
            String whitelistPath = proxyRule.getPath() + "/*";

            if (targetServiceUrl == null) {
                throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
            }
            ServletHolder proxyServlet = new ServletHolder(new ProxyHolder(service, proxyRule));
            proxyServlet.setInitParameter(PROXY_TO, targetServiceUrl);
            proxyServlet.setInitParameter(PREFIX, proxyRule.getPath());
            proxyServlet.setInitParameter(TIMEOUT, String.valueOf(config.getDefaultTimeout()));
            // demo

            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);

            if (basicAuthProvider.shouldEnableAuth(proxyRule)) {
                authenticators.add(new BasicAuthenticator());
                basicAuthSecurityHandler
                        .addConstraintMapping(basicAuthProvider
                        .createConstraintMapping
                                (whitelistPath, basicAuthProvider.getAuthRoles(proxyRule)));
            }
            if (shouldEnableForwardAuth(proxyRule)) {
                authenticators.add(new ForwardAuthAuthenticator());
                basicAuthSecurityHandler
                        .addConstraintMapping(createForwardAuthConstraintMapping
                                (whitelistPath, null));
            }
            multiLayerAuthenticator.registerAuthenticators(whitelistPath, authenticators);

        }
        basicAuthSecurityHandler.setAuthenticator(multiLayerAuthenticator);
        // Set each security handler to handle the context separately
        basicAuthSecurityHandler.setHandler(context);
        // Combine handlers in a HandlerCollection
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{
                basicAuthSecurityHandler, context});

        // FilterHolder metricsFilterHolder = new FilterHolder(new MetricFilter());
        // context.addFilter(metricsFilterHolder, "/*", null);
        server.setHandler(handlers);
    }

    public boolean shouldEnableForwardAuth(AppConfig.Proxy proxy) {
        return proxy.getMiddleware() != null && proxy.getMiddleware().hasForwardAuth();
    }
    public ConstraintMapping createForwardAuthConstraintMapping(String pathSpec, String role) {
        Constraint constraint = new Constraint();
        constraint.setName("forwardAuth");
        constraint.setAuthenticate(true); // Set authenticate to true
        constraint.setRoles(new String[]{"user", "admin"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec(pathSpec);
        mapping.setConstraint(constraint);

        return mapping;
    }
}