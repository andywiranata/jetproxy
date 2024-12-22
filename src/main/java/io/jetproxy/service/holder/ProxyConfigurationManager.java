package io.jetproxy.service.holder;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.ConfigLoader;
import io.jetproxy.middleware.auth.MultiLayerAuthenticator;
import io.jetproxy.middleware.auth.AuthProviderFactory;
import io.jetproxy.middleware.auth.BasicAuthProvider;
import io.jetproxy.middleware.log.AccessLog;
import io.jetproxy.middleware.rule.Rule;
import io.jetproxy.service.holder.handler.HttpCacheHandler;
import io.jetproxy.service.holder.handler.MiddlewareChain;
import io.jetproxy.service.holder.handler.MiddlewareHandler;
import io.jetproxy.service.holder.handler.RuleValidatorHandler;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.security.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

public class ProxyConfigurationManager {
    private static final String PROXY_TO = "proxyTo";
    private static final String PREFIX = "prefix";
    private static final String TIMEOUT = "timeout";
    private static final Logger logger = LoggerFactory.getLogger(ProxyConfigurationManager.class);
    private final AppConfig config;
    private final ServletContextHandler context;

    // Thread-safe map to hold dynamically added proxies
    private final ConcurrentHashMap<String, ServletHolder> dynamicProxies = new ConcurrentHashMap<>();

    /**
     * Constructor initializes the SetupProxyHolder with the application config and context.
     *
     * @param config  The application configuration containing proxy rules.
     * @param context The servlet context handler used to register servlets.
     */
    public ProxyConfigurationManager(AppConfig config, ServletContextHandler context) {
        this.config = config;
        this.context = context;
    }

    /**
     * Sets up initial proxies defined in the application configuration.
     *
     * @param server The Jetty server instance.
     * @param context The servlet context handler.
     */
    public void setupProxies(Server server, ServletContextHandler context) {
        List<AppConfig.Proxy> proxies = config.getProxies();
        MultiLayerAuthenticator multiLayerAuthenticator = new MultiLayerAuthenticator();

        // Basic Auth setup
        BasicAuthProvider basicAuthProvider = (BasicAuthProvider) AuthProviderFactory.getAuthProvider("basicAuth");
        ConstraintSecurityHandler basicAuthSecurityHandler = basicAuthProvider.createSecurityHandler(this.config);

        for (AppConfig.Proxy proxyRule : proxies) {
            AppConfig.Service service = ConfigLoader.getServiceMap().get(proxyRule.getService());

            // Skip invalid or missing services
            if (service == null) {
                logger.warn("Service not found for proxy: {}", proxyRule.getService());
                continue;
            }

            // Add the proxy servlet
            String targetServiceUrl = service.getUrl();
            String whitelistPath = proxyRule.getPath() + "/*";
            ServletHolder proxyServlet = createServletHolder(proxyRule, targetServiceUrl, service);
            context.addServlet(proxyServlet, whitelistPath);

            // Set up authentication if needed
            if (basicAuthProvider.shouldEnableAuth(proxyRule)) {
                basicAuthSecurityHandler.addConstraintMapping(
                        basicAuthProvider.createConstraintMapping(whitelistPath, basicAuthProvider.getAuthRoles(proxyRule))
                );
            }

            // Set up forward authentication if needed
            if (shouldEnableForwardAuth(proxyRule)) {
                basicAuthSecurityHandler.addConstraintMapping(
                        createForwardAuthConstraintMapping(whitelistPath, null)
                );
            }
        }
        // Add handlers for security and logging
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(new AccessLog());

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{
                basicAuthSecurityHandler,
                context,
                requestLogHandler
        });

        server.setHandler(handlers);
    }

    /**
     * Adds or updates a proxy dynamically at runtime.
     *
     * @param newProxy The new proxy configuration to add or update.
     */
    public synchronized void addOrUpdateProxy(AppConfig.Proxy newProxy) {
        String pathSpec = newProxy.getPath() + "/*";

        // Remove the existing proxy if it exists
        if (dynamicProxies.containsKey(pathSpec)) {
            removeProxy(newProxy.getPath());
        }

        // Add the new proxy
        AppConfig.Service service = ConfigLoader.getServiceMap().get(newProxy.getService());
        if (service == null) {
            throw new IllegalArgumentException("Service not found for: " + newProxy.getService());
        }

        ServletHolder proxyServlet = createServletHolder(newProxy, service.getUrl(), service);
        context.addServlet(proxyServlet, pathSpec);
        dynamicProxies.put(pathSpec, proxyServlet);
//        ConfigLoader.updateProxy(newProxy);

        logger.info("Proxy dynamically added/updated: {} -> {}", newProxy.getPath(), service.getUrl());
    }

    /**
     * Removes a proxy dynamically based on its path.
     *
     * @param path The proxy path to remove.
     */
    public synchronized void removeProxy(String path) {
        String pathSpec = path + "/*";

        ServletHolder holder = dynamicProxies.remove(pathSpec);
        if (holder != null) {
            try {
                // Remove the servlet holder and its mapping from the handler
                ServletHandler handler = context.getServletHandler();
                handler.setServlets(removeServletFromArray(handler.getServlets(), holder));
                handler.setServletMappings(removeMappingFromArray(handler.getServletMappings(), pathSpec));

                logger.info("Proxy removed dynamically: {}", path);
            } catch (Exception e) {
                logger.error("Failed to remove proxy dynamically: {}", path, e);
            }
        } else {
            logger.warn("No proxy found to remove for path: {}", path);
        }
    }

    /**
     * Checks if forward authentication is enabled for a proxy.
     *
     * @param proxy The proxy configuration.
     * @return True if forward authentication is enabled, otherwise false.
     */
    public boolean shouldEnableForwardAuth(AppConfig.Proxy proxy) {
        return proxy.getMiddleware() != null && proxy.getMiddleware().hasForwardAuth();
    }

    /**
     * Creates a forward authentication constraint mapping.
     *
     * @param pathSpec The path specification for the constraint.
     * @param role     The role associated with the constraint.
     * @return A ConstraintMapping object.
     */
    public ConstraintMapping createForwardAuthConstraintMapping(String pathSpec, String role) {
        Constraint constraint = new Constraint();
        constraint.setName("forwardAuth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{"user", "admin"});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec(pathSpec);
        mapping.setConstraint(constraint);

        return mapping;
    }

    /**
     * Helper method to create a servlet holder for a proxy.
     */
    private ServletHolder createServletHolder(AppConfig.Proxy proxyRule, String targetServiceUrl, AppConfig.Service service) {
        String proxyTo = targetServiceUrl + proxyRule.getPath();
        String prefix = proxyRule.getPath();
        String timeout = String.valueOf(config.getDefaultTimeout());

        MiddlewareChain middlewareChain = new MiddlewareChain(List.of(
                new RuleValidatorHandler(service, proxyRule),
                new HttpCacheHandler()
        ));
        ServletHolder proxyServlet = new ServletHolder(new ProxyRequestHandler(
                service, proxyRule, middlewareChain));
        proxyServlet.setInitParameter(PROXY_TO, proxyTo);
        proxyServlet.setInitParameter(PREFIX, prefix);
        proxyServlet.setInitParameter(TIMEOUT, timeout);

        logger.info("Proxy added: {} -> {}", proxyRule.getPath(), proxyTo);
        return proxyServlet;
    }

    /**
     * Helper method to remove a servlet holder from an array.
     */
    private ServletHolder[] removeServletFromArray(ServletHolder[] holders, ServletHolder toRemove) {
        return Arrays.stream(holders)
                .filter(holder -> !holder.equals(toRemove))
                .toArray(ServletHolder[]::new);
    }

    /**
     * Helper method to remove a servlet mapping from an array.
     */
    private ServletMapping[] removeMappingFromArray(ServletMapping[] mappings, String pathSpec) {
        return Arrays.stream(mappings)
                .filter(mapping -> !mapping.getPathSpecs().equals(pathSpec))
                .toArray(ServletMapping[]::new);
    }
}
