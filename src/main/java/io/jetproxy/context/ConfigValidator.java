package io.jetproxy.context;

import io.jetproxy.exception.JetProxyValidationException;
import org.eclipse.jetty.util.StringUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigValidator {


    /**
     * Validates the entire configuration.
     */
    public static void validateConfig(AppConfig config) {
        if (StringUtil.isEmpty(config.getAppName())) {
            throw new JetProxyValidationException("appName cannot be null");
        }
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new JetProxyValidationException("Invalid port number: " + config.getPort());
        }
        if (config.getDefaultTimeout() <= 0) {
            throw new JetProxyValidationException("Default timeout must be greater than 0");
        }
        if (StringUtil.isEmpty(config.getRootPath()) || !config.getRootPath().startsWith("/")) {
            throw new JetProxyValidationException("rootPath must start with '/'");
        }

        // Check if at least one service type is available
        boolean hasHttpServices = config.getServices() != null && !config.getServices().isEmpty();

        ConfigValidator.validateProxies(config.getProxies(), config.getServices(), config.getGrpcServices());

        if (hasHttpServices) {
            ConfigValidator.validateServices(config.getServices());
        } else {
            ConfigValidator.validateGrpServices(config.getGrpcServices());
        }

        if (!config.hasCorsFilter()) {
            throw new JetProxyValidationException("Cors Filter cannot be null");
        } else {
            if (config.getCorsFilter().getAccessControlAllowMethods() == null) {
                throw new JetProxyValidationException("Access Control Allow Methods cannot be null");
            }
            if (config.getCorsFilter().getAccessControlAllowOriginList() == null) {
                throw new JetProxyValidationException("Access Control Origin List cannot be null");
            }
            if (config.getCorsFilter().getAccessControlAllowHeaders() == null) {
                throw new JetProxyValidationException("Access Control Headers cannot be null");
            }
        }
        if (config.getUsers() != null) {
            for (AppConfig.User user : config.getUsers()) {
                // Validate username
                if (StringUtil.isEmpty(user.getUsername())) {
                    throw new JetProxyValidationException("Username cannot be null or empty for user: " + user.getUsername());
                }
                // Validate password
                if (StringUtil.isEmpty(user.getPassword())) {
                    throw new JetProxyValidationException("Password cannot be null or empty for user: " + user.getUsername());
                }
                // Validate role
                if (StringUtil.isEmpty(user.getRole())) {
                    throw new JetProxyValidationException("Role cannot be null or empty for user: " + user.getUsername());
                }
            }
        }

    }
    public static void validateGrpServices(List<AppConfig.GrpcService> services) {
        if (services == null || services.isEmpty()) {
           return;
        }
        // Ensure service names are unique
        Set<String> uniqueServiceNames = new HashSet<>();

        for (AppConfig.GrpcService grpcService : services) {
            if (grpcService.getName() == null || grpcService.getName().isEmpty()) {
                throw new JetProxyValidationException("GrpcService name cannot be null or empty");
            }

            if (!uniqueServiceNames.add(grpcService.getName())) {
                throw new JetProxyValidationException("Duplicate GrpcService name found: " + grpcService.getName());
            }

            // Validate service URL
            if (grpcService.getHost() == null || grpcService.getHost().isEmpty()) {
                throw new JetProxyValidationException("GrpcService Host cannot be null or empty for service: " + grpcService.getHost());
            }
            if (grpcService.getPort() == null) {
                throw new JetProxyValidationException("GrpcService Port cannot be null or empty for service: " + grpcService.getPort());
            }


        }
    }
    /**
     * Validates the list of services for correctness and uniqueness.
     *
     * @param services The list of services to validate.
     */
    public static void validateServices(List<AppConfig.Service> services) {
        if (services == null || services.isEmpty()) {
            throw new JetProxyValidationException("No services configured");
        }

        // Ensure service names are unique
        Set<String> uniqueServiceNames = new HashSet<>();

        for (AppConfig.Service service : services) {
            // Validate service name
            if (service.getName() == null || service.getName().isEmpty()) {
                throw new JetProxyValidationException("Service name cannot be null or empty");
            }
            if (!uniqueServiceNames.add(service.getName())) {
                throw new JetProxyValidationException("Duplicate service name found: " + service.getName());
            }

            // Validate service URL
            if (service.getUrl() == null || service.getUrl().isEmpty()) {
                throw new JetProxyValidationException("Service URL cannot be null or empty for service: " + service.getName());
            }
            if (!service.getUrl().startsWith("http://") && !service.getUrl().startsWith("https://")) {
                throw new JetProxyValidationException("Service URL must start with 'http://' or 'https://': " + service.getUrl());
            }

            // Validate HTTP methods if provided
            if (service.getMethods() != null) {
                if (service.getMethods().isEmpty()) {
                    throw new JetProxyValidationException("HTTP methods cannot be empty for service: " + service.getName());
                }

                // Ensure all methods are valid HTTP methods
                for (String method : service.getMethods()) {
                    if (!isValidHttpMethod(method)) {
                        throw new JetProxyValidationException("Invalid HTTP method '" + method + "' for service: " + service.getName());
                    }
                }
            }

            // Validate healthcheck path if provided
            if (service.getHealthcheck() != null && !service.getHealthcheck().startsWith("/")) {
                throw new JetProxyValidationException("Healthcheck path must start with '/': " + service.getHealthcheck());
            }
        }
    }

    /**
     * Checks if a given method is a valid HTTP method.
     *
     * @param method The HTTP method to validate.
     * @return True if valid, false otherwise.
     */
    private static boolean isValidHttpMethod(String method) {
        return switch (method.toUpperCase()) {
            case "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE", "CONNECT" -> true;
            default -> false;
        };
    }
    /**
     * Validates the list of proxies and ensures they reference valid HTTP or gRPC services.
     */
    public static void validateProxies(List<AppConfig.Proxy> proxies, List<AppConfig.Service> services, List<AppConfig.GrpcService> grpcServices) {
        if (proxies == null || proxies.isEmpty()) {
            throw new JetProxyValidationException("No proxies configured");
        }

        if ((services == null || services.isEmpty()) && (grpcServices == null || grpcServices.isEmpty())) {
            throw new JetProxyValidationException("No services configured (HTTP or gRPC), but proxies depend on them.");
        }

        // Collect registered HTTP and gRPC service names
        Set<String> registeredServiceNames = new HashSet<>();
        if (services != null) {
            registeredServiceNames.addAll(services.stream().map(AppConfig.Service::getName).collect(Collectors.toSet()));
        }
        if (grpcServices != null) {
            registeredServiceNames.addAll(grpcServices.stream().map(AppConfig.GrpcService::getName).collect(Collectors.toSet()));
        }

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
            validateMatches(proxy, registeredServiceNames);
            validateMiddleware(proxy, registeredServiceNames);
        }
    }

    /**
     * Validates the matches list inside proxies.
     */
    public static void validateMatches(AppConfig.Proxy proxy, Set<String> registeredServiceNames) {
        if (proxy.getMatches() == null || proxy.getMatches().isEmpty()) {
            return; // No matches to validate
        }

        for (AppConfig.Match match : proxy.getMatches()) {
            if (match.getRule() == null || match.getRule().isEmpty()) {
                throw new JetProxyValidationException("Match rule cannot be null or empty in proxy: " + proxy.getPath());
            }
            if (match.getService() == null || match.getService().isEmpty()) {
                throw new JetProxyValidationException("Match service cannot be null or empty in proxy: " + proxy.getPath());
            }
            if (!registeredServiceNames.contains(match.getService())) {
                throw new JetProxyValidationException("Match references an unregistered service: " + match.getService()
                        + " in proxy: " + proxy.getPath());
            }
        }
    }

    public static void validateMiddleware(AppConfig.Proxy proxy,  Set<String> registeredServiceNames) {
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
            if (registeredServiceNames != null && !registeredServiceNames.contains(forwardAuth.getService())) {
                throw new JetProxyValidationException("ForwardAuth middleware is enabled, but the specified service ('"
                        + forwardAuth.getService() + "') is not registered in the service list.");
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

        // Validate Idempotency Middleware
        AppConfig.Idempotency idempotency = middleware.getIdempotency();
        if (idempotency != null) {
            if (idempotency.getHeaderName() == null || idempotency.getHeaderName().isEmpty()) {
                throw new JetProxyValidationException("Idempotency middleware is enabled but header name are missing.");
            }
        }

        AppConfig.Mirroring mirroring = middleware.getMirroring();
        if (mirroring != null) {
            if (mirroring.getMirrorService() == null || mirroring.getMirrorService().isEmpty()) {
                throw new JetProxyValidationException("Mirroring middleware is enabled but Service are missing.");
            }
        }
    }
}
