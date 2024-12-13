package io.jetproxy.service.holder;

import io.jetproxy.middleware.auth.ForwardAuthAuthenticator;
import io.jetproxy.middleware.auth.MultiLayerAuthenticator;
import io.jetproxy.middleware.log.AccessLog;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.ConfigLoader;

import io.jetproxy.middleware.auth.AuthProviderFactory;
import io.jetproxy.middleware.auth.BasicAuthProvider;

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
            ServletHolder proxyServlet = getServletHolder(proxyRule, targetServiceUrl, service);

            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");

            if (basicAuthProvider.shouldEnableAuth(proxyRule)) {
                authenticators.add(new BasicAuthenticator());
                basicAuthSecurityHandler
                        .addConstraintMapping(basicAuthProvider
                        .createConstraintMapping
                                (whitelistPath,
                                        basicAuthProvider.getAuthRoles(proxyRule)));
            }
            if (shouldEnableForwardAuth(proxyRule)) {
                authenticators.add(new ForwardAuthAuthenticator(proxyRule.getMiddleware()));
                basicAuthSecurityHandler
                        .addConstraintMapping(createForwardAuthConstraintMapping
                                (whitelistPath,
                                        null));
            }
            multiLayerAuthenticator.registerAuthenticators(whitelistPath, authenticators);
        }
        basicAuthSecurityHandler.setAuthenticator(multiLayerAuthenticator);
        basicAuthSecurityHandler.setHandler(context);

        // Log Handler
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(new AccessLog());

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{
                basicAuthSecurityHandler, context, requestLogHandler});

        server.setHandler(handlers);
    }

    private ServletHolder getServletHolder(AppConfig.Proxy proxyRule, String targetServiceUrl, AppConfig.Service service) {
        String proxyTo = targetServiceUrl + proxyRule.getPath();
        String prefix = proxyRule.getPath();
        String timeout = String.valueOf(config.getDefaultTimeout());

        if (targetServiceUrl == null) {
            throw new IllegalArgumentException("Service URL not found for: " + proxyRule.getService());
        }
        ServletHolder proxyServlet = new ServletHolder(new ProxyHolder(service, proxyRule));
        proxyServlet.setInitParameter(PROXY_TO, proxyTo);
        proxyServlet.setInitParameter(PREFIX, prefix);
        proxyServlet.setInitParameter(TIMEOUT, timeout);

        logger.info("Proxy added: {} -> {}", proxyRule.getPath(), proxyTo);
        return proxyServlet;
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