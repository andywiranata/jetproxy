package io.jetproxy.context;

import io.jetproxy.exception.JetProxyValidationException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigValidator {


    /**
     * Validates the entire configuration.
     */
    public static void validateConfig(AppConfig config) {
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new JetProxyValidationException("Invalid port number: " + config.getPort());
        }
        if (config.getDefaultTimeout() <= 0) {
            throw new JetProxyValidationException("Default timeout must be greater than 0");
        }
        ConfigValidator.validateProxies(config.getProxies(), config.getServices());
        ConfigValidator.validateServices(config.getServices());
    }

    /**
     * Validates the list of services.
     */
    public static void validateServices(List<AppConfig.Service> services) {
        if (services == null || services.isEmpty()) {
            throw new JetProxyValidationException("No services configured");
        }

        for (AppConfig.Service service : services) {
            if (service.getName() == null || service.getName().isEmpty()) {
                throw new JetProxyValidationException("Service name cannot be null or empty");
            }
            if (service.getUrl() == null || service.getUrl().isEmpty()) {
                throw new JetProxyValidationException("Service URL cannot be null or empty for service: " + service.getName());
            }
            if (!service.getUrl().startsWith("http://") && !service.getUrl().startsWith("https://")) {
                throw new JetProxyValidationException("Service URL must start with 'http://' or 'https://': " + service.getUrl());
            }
        }
    }
    /**
     * Validates the list of proxies and ensures they reference valid services.
     */
    public static void validateProxies(List<AppConfig.Proxy> proxies, List<AppConfig.Service> services) {
        if (proxies == null || proxies.isEmpty()) {
            throw new JetProxyValidationException("No proxies configured");
        }

        if (services == null || services.isEmpty()) {
            throw new JetProxyValidationException("No services configured, but proxies depend on them.");
        }

        Set<String> registeredServiceNames = services.stream()
                .map(AppConfig.Service::getName)
                .collect(Collectors.toSet());

        for (AppConfig.Proxy proxy : proxies) {
            if (proxy.getPath() == null || proxy.getPath().isEmpty()) {
                throw new JetProxyValidationException("Proxy path cannot be null or empty");
            }
            if (proxy.getService() == null || proxy.getService().isEmpty()) {
                throw new JetProxyValidationException("Proxy service cannot be null or empty for path: " + proxy.getPath());
            }
            if (!registeredServiceNames.contains(proxy.getService())) {
                throw new JetProxyValidationException("Proxy references an unregistered service: " + proxy.getService()
                        + " for path: " + proxy.getPath());
            }
            if (!proxy.getPath().startsWith("/")) {
                throw new JetProxyValidationException("Proxy path must start with '/': " + proxy.getPath());
            }
        }
    }

    public static void validateMiddleware(AppConfig.Proxy proxy) {
        AppConfig.Middleware middleware = proxy.getMiddleware();

        if (middleware == null) {
            return;
        }

        // Validate BasicAuth middleware
        if (middleware.getBasicAuth() != null && middleware.getBasicAuth().isEmpty()) {
            throw new JetProxyValidationException("BasicAuth middleware is enabled but has no roles specified.");
        }

        // Validate ForwardAuth middleware
        AppConfig.ForwardAuth forwardAuth = middleware.getForwardAuth();
        if (forwardAuth != null) {
            if (forwardAuth.getPath() == null || forwardAuth.getPath().isEmpty()) {
                throw new JetProxyValidationException("ForwardAuth middleware is enabled but path is missing.");
            }
            if (forwardAuth.getService() == null || forwardAuth.getService().isEmpty()) {
                throw new JetProxyValidationException("ForwardAuth middleware is enabled but service is missing.");
            }
            if (forwardAuth.getRequestHeaders() == null || forwardAuth.getRequestHeaders().isEmpty()) {
                throw new JetProxyValidationException("ForwardAuth middleware is enabled but requestHeaders are missing.");
            }
            if (forwardAuth.getResponseHeaders() == null || forwardAuth.getResponseHeaders().isEmpty()) {
                throw new JetProxyValidationException("ForwardAuth middleware is enabled but responseHeaders are missing.");
            }
        }

        // Validate RateLimiter middleware
        AppConfig.RateLimiter rateLimiter = middleware.getRateLimiter();
        if (rateLimiter != null && rateLimiter.isEnabled()) {
            if (rateLimiter.getLimitRefreshPeriod() <= 0) {
                throw new JetProxyValidationException("RateLimiter is enabled but limitRefreshPeriod is invalid.");
            }
            if (rateLimiter.getLimitForPeriod() <= 0) {
                throw new JetProxyValidationException("RateLimiter is enabled but limitForPeriod is invalid.");
            }
            if (rateLimiter.getMaxBurstCapacity() <= 0) {
                throw new JetProxyValidationException("RateLimiter is enabled but maxBurstCapacity is invalid.");
            }
        }

        // Validate CircuitBreaker middleware
        AppConfig.CircuitBreaker circuitBreaker = middleware.getCircuitBreaker();
        if (circuitBreaker != null && circuitBreaker.isEnabled()) {
            if (circuitBreaker.getFailureThreshold() <= 0 || circuitBreaker.getFailureThreshold() > 100) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but failureThreshold is invalid.");
            }
            if (circuitBreaker.getSlowCallThreshold() <= 0 || circuitBreaker.getSlowCallThreshold() > 100) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but slowCallThreshold is invalid.");
            }
            if (circuitBreaker.getSlowCallDuration() <= 0) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but slowCallDuration is invalid.");
            }
            if (circuitBreaker.getOpenStateDuration() <= 0) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but openStateDuration is invalid.");
            }
            if (circuitBreaker.getWaitDurationInOpenState() <= 0) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but waitDurationInOpenState is invalid.");
            }
            if (circuitBreaker.getPermittedNumberOfCallsInHalfOpenState() <= 0) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but permittedNumberOfCallsInHalfOpenState is invalid.");
            }
            if (circuitBreaker.getMinimumNumberOfCalls() <= 0) {
                throw new JetProxyValidationException("CircuitBreaker is enabled but minimumNumberOfCalls is invalid.");
            }
        }

        // Validate Header middleware
        AppConfig.Headers header = middleware.getHeader();
        if (header != null) {
            if (header.getRequestHeaders() == null || header.getRequestHeaders().isEmpty()) {
                throw new JetProxyValidationException("Header middleware is enabled but requestHeaders are missing.");
            }
            if (header.getResponseHeaders() == null || header.getResponseHeaders().isEmpty()) {
                throw new JetProxyValidationException("Header middleware is enabled but responseHeaders are missing.");
            }
        }

    }
}
