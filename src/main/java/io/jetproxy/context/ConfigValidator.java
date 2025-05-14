package io.jetproxy.context;

import io.jetproxy.exception.JetProxyValidationException;
import io.jetproxy.util.FatalValidationHints;
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
            FatalValidationHints.missingAppName();
        }
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            FatalValidationHints.invalidPort(config.getPort());
        }
        if (config.getDefaultTimeout() <= 0) {
            FatalValidationHints.invalidTimeout(config.getDefaultTimeout());
        }
        if (StringUtil.isEmpty(config.getRootPath()) || !config.getRootPath().startsWith("/")) {
            FatalValidationHints.invalidRootPath(config.getRootPath());
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
            FatalValidationHints.missingCorsFilter();
        } else {
            if (config.getCorsFilter().getAccessControlAllowMethods() == null) {
               FatalValidationHints.missingCorsMethods();
            }
            if (config.getCorsFilter().getAccessControlAllowOriginList() == null) {
                FatalValidationHints.missingCorsOrigins();
            }
            if (config.getCorsFilter().getAccessControlAllowHeaders() == null) {
                FatalValidationHints.missingCorsHeaders();
            }
        }
        if (config.getUsers() != null) {
            for (AppConfig.User user : config.getUsers()) {
                String userRef = user.getUsername() != null ? "'" + user.getUsername() + "'" : "<unknown>";
                // Validate username
                if (StringUtil.isEmpty(user.getUsername())) {
                    FatalValidationHints.missingUsername();
                }
                // Validate password
                if (StringUtil.isEmpty(user.getPassword())) {
                    FatalValidationHints.missingPassword(userRef);
                }
                // Validate role
                if (StringUtil.isEmpty(user.getRole())) {
                    FatalValidationHints.missingRole(userRef);
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
                FatalValidationHints.grpcServiceNameMissing();
            }

            if (!uniqueServiceNames.add(grpcService.getName())) {
                FatalValidationHints.duplicateGrpcServiceName(grpcService.getName());
            }
            // Validate service URL
            if (grpcService.getHost() == null || grpcService.getHost().isEmpty()) {
                FatalValidationHints.grpcServiceHostMissing(grpcService.getHost());
            }
            if (grpcService.getPort() == null) {
                FatalValidationHints.grpcServicePortMissing(grpcService.getName());
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
            FatalValidationHints.noServicesConfigured();
        }

        Set<String> uniqueServiceNames = new HashSet<>();

        for (AppConfig.Service service : services) {
            String name = service.getName();

            if (name == null || name.isBlank()) {
                FatalValidationHints.missingServiceName();
            }

            if (!uniqueServiceNames.add(name)) {
                FatalValidationHints.duplicateServiceName(name);
            }

            if (service.getUrl() == null || service.getUrl().isBlank()) {
                FatalValidationHints.missingServiceUrl(name);
            }

            if (!service.getUrl().startsWith("http://") && !service.getUrl().startsWith("https://")) {
                FatalValidationHints.invalidServiceUrl(service.getUrl());
            }

            if (service.getMethods() != null) {
                if (service.getMethods().isEmpty()) {
                    FatalValidationHints.emptyHttpMethods(name);
                }

                for (String method : service.getMethods()) {
                    if (!isValidHttpMethod(method)) {
                        FatalValidationHints.invalidHttpMethod(method, name);
                    }
                }
            }

            if (service.getHealthcheck() != null && !service.getHealthcheck().startsWith("/")) {
                FatalValidationHints.invalidHealthcheckPath(service.getHealthcheck());
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
            FatalValidationHints.noProxiesConfigured();
        }

        if ((services == null || services.isEmpty()) && (grpcServices == null || grpcServices.isEmpty())) {
            FatalValidationHints.noServicesForProxies();
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
                FatalValidationHints.proxyPathMissing();
            }
            if (proxy.getService() == null || proxy.getService().isEmpty()) {
                FatalValidationHints.proxyServiceMissing(proxy.getPath());
            }
            if (!registeredServiceNames.contains(proxy.getService())) {
                FatalValidationHints.proxyServiceUnregistered(proxy.getService(), proxy.getPath());
            }
            if (!proxy.getPath().startsWith("/")) {
                FatalValidationHints.proxyPathMustStartWithSlash(proxy.getPath());
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
                FatalValidationHints.matchRuleMissing(proxy.getPath());
            }
            if (match.getService() == null || match.getService().isEmpty()) {
                FatalValidationHints.matchServiceMissing(proxy.getPath());
            }
            if (!registeredServiceNames.contains(match.getService())) {
                FatalValidationHints.matchServiceUnregistered(match.getService(), proxy.getPath());

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
            FatalValidationHints.basicAuthRolesMissing();
        }

        // Validate ForwardAuth middleware
        AppConfig.ForwardAuth forwardAuth = middleware.getForwardAuth();
        if (forwardAuth != null) {
            if (forwardAuth.getPath() == null || forwardAuth.getPath().isEmpty()) {
                FatalValidationHints.forwardAuthPathMissing();
            }
            if (forwardAuth.getService() == null || forwardAuth.getService().isEmpty()) {
                FatalValidationHints.forwardAuthServiceMissing();
            }
            if (registeredServiceNames != null && !registeredServiceNames.contains(forwardAuth.getService())) {
                FatalValidationHints.forwardAuthServiceUnregistered(forwardAuth.getService());
            }
            if (forwardAuth.getRequestHeaders() == null || forwardAuth.getRequestHeaders().isEmpty()) {
                FatalValidationHints.forwardAuthRequestHeadersMissing();
            }
            if (forwardAuth.getResponseHeaders() == null || forwardAuth.getResponseHeaders().isEmpty()) {
                FatalValidationHints.forwardAuthResponseHeadersMissing();
            }
        }
        AppConfig.RateLimiter rateLimiter = middleware.getRateLimiter();
        if (rateLimiter != null && rateLimiter.isEnabled()) {
            if (rateLimiter.getLimitRefreshPeriod() <= 0) {
                FatalValidationHints.rateLimiterInvalidRefreshPeriod();
            }
            if (rateLimiter.getLimitForPeriod() <= 0) {
                FatalValidationHints.rateLimiterInvalidLimitForPeriod();
            }
            if (rateLimiter.getMaxBurstCapacity() <= 0) {
                FatalValidationHints.rateLimiterInvalidBurstCapacity();
            }
        }
        AppConfig.CircuitBreaker circuitBreaker = middleware.getCircuitBreaker();
        if (circuitBreaker != null && circuitBreaker.isEnabled()) {
            if (circuitBreaker.getFailureThreshold() <= 0 || circuitBreaker.getFailureThreshold() > 100) {
                FatalValidationHints.circuitBreakerInvalidFailureThreshold();
            }
            if (circuitBreaker.getSlowCallThreshold() <= 0 || circuitBreaker.getSlowCallThreshold() > 100) {
                FatalValidationHints.circuitBreakerInvalidSlowCallThreshold();
            }
            if (circuitBreaker.getSlowCallDuration() <= 0) {
                FatalValidationHints.circuitBreakerInvalidSlowCallDuration();
            }
            if (circuitBreaker.getOpenStateDuration() <= 0) {
                FatalValidationHints.circuitBreakerInvalidOpenStateDuration();
            }
            if (circuitBreaker.getWaitDurationInOpenState() <= 0) {
                FatalValidationHints.circuitBreakerInvalidWaitDuration();
            }
            if (circuitBreaker.getPermittedNumberOfCallsInHalfOpenState() <= 0) {
                FatalValidationHints.circuitBreakerInvalidHalfOpenCalls();
            }
            if (circuitBreaker.getMinimumNumberOfCalls() <= 0) {
                FatalValidationHints.circuitBreakerInvalidMinimumCalls();
            }
        }


        AppConfig.Headers header = middleware.getHeader();
        if (header != null) {
            if (header.getRequestHeaders() == null || header.getRequestHeaders().isEmpty()) {
                FatalValidationHints.headerRequestHeadersMissing();
            }
            if (header.getResponseHeaders() == null || header.getResponseHeaders().isEmpty()) {
                FatalValidationHints.headerResponseHeadersMissing();
            }
        }

        AppConfig.Idempotency idempotency = middleware.getIdempotency();
        if (idempotency != null) {
            if (idempotency.getHeaderName() == null || idempotency.getHeaderName().isEmpty()) {
                FatalValidationHints.idempotencyHeaderMissing();
            }
        }

        AppConfig.Mirroring mirroring = middleware.getMirroring();
        if (mirroring != null) {
            if (mirroring.getMirrorService() == null || mirroring.getMirrorService().isEmpty()) {
                FatalValidationHints.mirroringServiceMissing();
            }
        }

    }
}
