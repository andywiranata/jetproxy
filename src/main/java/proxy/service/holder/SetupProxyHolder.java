package proxy.service.holder;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;
import proxy.context.ConfigLoader;

import proxy.middleware.auth.AuthProviderFactory;
import proxy.middleware.auth.BasicAuthProvider;

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
        BasicAuthProvider basicAuthProvider = (BasicAuthProvider) AuthProviderFactory.getAuthProvider("basicAuth");
        ConstraintSecurityHandler basicAuthSecurityHandler = basicAuthProvider.createSecurityHandler(this.config);

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
            // demo

            context.addServlet(proxyServlet, proxyRule.getPath() + "/*");
            logger.info("Proxy added: {} -> {}", proxyRule.getPath(), targetServiceUrl);

            if (basicAuthProvider.shouldEnableAuth(proxyRule)) {
                ConstraintMapping basicAuthMapping = basicAuthProvider.createConstraintMapping(whitelistPath, basicAuthProvider.getAuthRoles(proxyRule));
                basicAuthSecurityHandler.addConstraintMapping(basicAuthMapping);
            }
        }
        // Set each security handler to handle the context separately
        basicAuthSecurityHandler.setHandler(context);

        // Combine handlers in a HandlerCollection
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{
                basicAuthSecurityHandler, context});

        // FilterHolder metricsFilterHolder = new FilterHolder(new MetricFilter());
        // context.addFilter(metricsFilterHolder, "/*", null);

        server.setHandler(handlers);
//
//
    }

}
