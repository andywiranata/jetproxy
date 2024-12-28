package io.jetproxy.service.holder;

import io.jetproxy.context.*;
import io.jetproxy.exception.JetProxyValidationException;
import io.jetproxy.middleware.auth.*;
import io.jetproxy.middleware.log.AccessLog;
import io.jetproxy.middleware.handler.HttpCacheHandler;
import io.jetproxy.middleware.handler.MiddlewareChain;
import io.jetproxy.middleware.handler.RuleValidatorHandler;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
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

import java.util.ArrayList;
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
    private final MultiLayerAuthenticator multiLayerAuthenticator;
    private HandlerCollection handlers;
    ConstraintSecurityHandler proxyAndsecurityHandler;
    BasicAuthProvider basicAuthProvider;
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
        this.multiLayerAuthenticator = new MultiLayerAuthenticator();
        this.handlers = new HandlerCollection();
        this.basicAuthProvider = (BasicAuthProvider) AuthProviderFactory.getAuthProvider("basicAuth");;
        this.proxyAndsecurityHandler = basicAuthProvider.createSecurityHandler(config);
    }

    /**
     * Sets up initial proxies defined in the application configuration.
     *
     * @param server The Jetty server instance.
     * @param context The servlet context handler.
     */
    public void setupProxiesAndAdminApi(Server server, ServletContextHandler context) {
        List<AppConfig.Proxy> proxies = config.getProxies();

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
            List<Authenticator> authenticators = new ArrayList<>();
            ServletHolder proxyServlet = createServletHolder(proxyRule, targetServiceUrl, service);
            context.addServlet(proxyServlet, whitelistPath);

            // Set up authentication if needed
            if (basicAuthProvider.shouldEnableAuth(proxyRule)) {
                authenticators.add(new BasicAuthenticator());
                this.proxyAndsecurityHandler.addConstraintMapping(
                        basicAuthProvider
                                .createConstraintMapping(whitelistPath,
                                        basicAuthProvider.getAuthRoles(proxyRule))
                );
            }

            // Set up forward authentication if needed
            if (shouldEnableForwardAuth(proxyRule)) {
                authenticators.add(new ForwardAuthAuthenticator(proxyRule.getMiddleware()));
            }
            if (shouldEnableJwtAuth(proxyRule)) {
                authenticators.add(new JWTAuthAuthenticator());
            }
            multiLayerAuthenticator.registerAuthenticators(whitelistPath, authenticators);
        }

        this.proxyAndsecurityHandler.setAuthenticator(multiLayerAuthenticator);
        this.proxyAndsecurityHandler.setHandler(context);

        addAdminSecurityHandler(context);

        // Add handlers for security and logging
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(new AccessLog());

        handlers.setHandlers(new Handler[]{
                this.proxyAndsecurityHandler,
                requestLogHandler
        });

        server.setHandler(handlers);
    }
    /**
     * Add a dedicated Basic Authentication security handler for /admin/*.
     */
    private void addAdminSecurityHandler(ServletContextHandler context) {
        String adminPath = "/admin/*";
        logger.info("Setting up Basic Authentication for path: {}", adminPath);

        // Create and configure a security handler for /admin/*
        ConstraintSecurityHandler adminSecurityHandler = basicAuthProvider.createSecurityHandler(config);
        adminSecurityHandler.addConstraintMapping(basicAuthProvider
                .createConstraintMapping(adminPath,"administrator"));
;
        adminSecurityHandler.setAuthenticator(new BasicAuthenticator());

        // Add the admin security handler to the context
        context.setSecurityHandler(adminSecurityHandler);
    }

    /**
     * Adds or updates a proxy dynamically at runtime.
     *
     * @param newProxy The new proxy configuration to add or update.
     */
    public synchronized void addOrUpdateProxy(AppConfig.Proxy newProxy) {

        ConfigValidator.validateProxies(List.of(newProxy), config.getServices());
        ConfigValidator.validateMiddleware(newProxy);

        String pathSpec = newProxy.getPath() + "/*";
        List<Authenticator> authenticators = new ArrayList<>();
        boolean isRequiredRestart = false;

        // Remove the existing proxy if it exists
        if (dynamicProxies.containsKey(pathSpec)) {
            logger.info("Updating existing proxy for path: {}", newProxy.getPath());
            removeProxy(newProxy.getPath());
        }

        // Fetch service configuration
        AppConfig.Service service = ConfigLoader.getServiceMap().get(newProxy.getService());
        if (service == null) {
            throw new JetProxyValidationException("Service not found for: " + newProxy.getService());
        }

        // Add the new proxy
        ServletHolder proxyServlet = createServletHolder(newProxy, service.getUrl(), service);
        try {
            context.addServlet(proxyServlet, pathSpec);
            dynamicProxies.put(pathSpec, proxyServlet);
            // Set up authentication if needed
            if (basicAuthProvider.shouldEnableAuth(newProxy)) {
                authenticators.add(new BasicAuthenticator());
                this.proxyAndsecurityHandler.addConstraintMapping(
                        basicAuthProvider
                                .createConstraintMapping(pathSpec,
                                        basicAuthProvider.getAuthRoles(newProxy))
                );
                isRequiredRestart = true;
            }
            if (shouldEnableForwardAuth(newProxy)) {
                authenticators.add(new ForwardAuthAuthenticator(newProxy.getMiddleware()));
                isRequiredRestart = true;
            }

            if (isRequiredRestart) {
                if (this.proxyAndsecurityHandler.isStarted()) {
                    this.proxyAndsecurityHandler.stop();
                    AppContext.get().preventGracefullyShutdown();
                }

                multiLayerAuthenticator.registerAuthenticators(pathSpec, authenticators);
                this.proxyAndsecurityHandler.setAuthenticator(multiLayerAuthenticator);

                if (!this.proxyAndsecurityHandler.isRunning()) {
                    this.proxyAndsecurityHandler.start();
                    AppContext.get().allowGracefullyShutdown();
                }
            }

            // Update in config loader
            ConfigLoader.addOrUpdateProxies(List.of(newProxy));
            logger.info("Proxy dynamically added/updated: {} -> {}", newProxy.getPath(), service.getUrl());
        } catch (Exception e) {
            logger.error("Failed to add or update proxy: {}", newProxy.getPath(), e);
            // Rollback on failure
            dynamicProxies.remove(pathSpec);
            try {
                context.getServletHandler().setServletMappings(removeMappingFromArray(
                        context.getServletHandler().getServletMappings(), pathSpec));
            } catch (Exception cleanupException) {
                logger.error("Failed to clean up after adding proxy: {}", pathSpec, cleanupException);
            }
            throw new RuntimeException("Failed to add or update proxy for path: " + newProxy.getPath(), e);
        }
    }



    /**
     * Removes a proxy dynamically based on its path.
     *
     * @param path The proxy path to remove.
     */
    public synchronized void removeProxy(String path) {
        String pathSpec = path + "/*";
        logger.info("Attempting to remove proxy for path: {}", path);
        ServletHolder holder = dynamicProxies.remove(pathSpec);
        if (holder != null) {
            logger.info("Found proxy holder for pathSpec: {}", pathSpec);
            try {
                ServletHandler handler = context.getServletHandler();
                if (handler == null) {
                    logger.error("ServletHandler is null. Unable to proceed with removal for path: {}", path);
                    dynamicProxies.put(pathSpec, holder); // Restore on failure
                    return;
                }

                // Log existing servlets and mappings before modification
                logger.info("Existing servlets: {}", Arrays.toString(handler.getServlets()));
                logger.info("Existing mappings: {}", Arrays.toString(handler.getServletMappings()));

                // Remove the servlet holder
                ServletHolder[] updatedHolders = Arrays.stream(handler.getServlets())
                        .filter(h -> !h.equals(holder))
                        .toArray(ServletHolder[]::new);
                handler.setServlets(updatedHolders);
                logger.info("Updated servlets after removal: {}", Arrays.toString(updatedHolders));

                // Remove the servlet mapping
                ServletMapping[] updatedMappings = Arrays.stream(handler.getServletMappings())
                        .filter(mapping -> !Arrays.asList(mapping.getPathSpecs()).contains(pathSpec))
                        .toArray(ServletMapping[]::new);
                handler.setServletMappings(updatedMappings);
                logger.debug("Updated mappings after removal: {}", Arrays.toString(updatedMappings));

                logger.info("Proxy removed dynamically for path: {}", path);
            } catch (Exception e) {
                logger.error("Failed to remove proxy dynamically for path: {}", path, e);

                // Revert the removal in case of failure
                dynamicProxies.put(pathSpec, holder);
                logger.debug("Restored proxy holder for pathSpec: {} due to failure.", pathSpec);
            }
        } else {
            logger.warn("No proxy found to remove for path: {}", path);
        }
    }

    public boolean shouldEnableJwtAuth(AppConfig.Proxy proxy) {
        return proxy.getMiddleware() != null && proxy.getMiddleware().hasJwtAuth();
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
     * Helper method to remove a servlet mapping from an array.
     */
    private ServletMapping[] removeMappingFromArray(ServletMapping[] mappings, String pathSpec) {
        return Arrays.stream(mappings)
                .filter(mapping -> !mapping.getPathSpecs().equals(pathSpec))
                .toArray(ServletMapping[]::new);
    }
}
