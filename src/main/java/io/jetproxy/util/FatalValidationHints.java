package io.jetproxy.util;

import io.jetproxy.exception.JetProxyValidationException;

/**
 * Centralized fatal error hints for configuration validation in JetProxy.
 */
public class FatalValidationHints {

    public static void missingAppName() {
        final String msg = "Missing 'appName' in config.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set a unique name for your JetProxy app.")
                        .example("appName: jetproxy-gateway")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidPort(int actualPort) {
        final String msg = "Invalid port number: " + actualPort;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Port must be in range 1–65535.")
                        .example("port: 8080")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidTimeout(int actualTimeout) {
        final String msg = "Invalid default timeout: " + actualTimeout;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Set a positive timeout value in milliseconds.")
                        .example("defaultTimeout: 10000")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidRootPath(String path) {
        final String msg = "Invalid rootPath: " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid rootPath: " + path)
                        .hint("It must start with '/'.")
                        .example("rootPath: /")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsFilter() {
        final String msg = "Missing CORS filter config.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Missing CORS filter config.")
                        .hint("Define global CORS settings under `corsFilter:` block.")
                        .example("""
                    corsFilter:
                      accessControlAllowMethods: ['GET', 'POST']
                      accessControlAllowHeaders: ['*']
                      accessControlAllowOriginList: ['*']
                    """)
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsMethods() {
        final String msg = "CORS: 'accessControlAllowMethods' is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Defines allowed HTTP methods for cross-origin requests.")
                        .example("accessControlAllowMethods: [*]")
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsOrigins() {
        String msg = "CORS: 'accessControlAllowOriginList' is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowOriginList' is missing.")
                        .hint("List domains allowed to make cross-origin requests.")
                        .example("accessControlAllowOriginList: ['https://example.com']")
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingCorsHeaders() {
        String msg = "CORS: 'accessControlAllowHeaders' is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowHeaders' is missing.")
                        .hint("Specify headers that clients may include in requests.")
                        .example("accessControlAllowHeaders: ['Content-Type', 'Authorization']")
                        .doc("middleware/cors")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingUsername(String userRef) {
        final String msg = "User config: username is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each user must have a unique username.")
                        .example("username: userA")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingPassword(String userRef) {
        final String msg = "User " + userRef + ": password is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Define a plaintext password or use secure storage.")
                        .example("password: secret123")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingRole(String userRef) {
        final String msg = "User " + userRef + ": role is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User " + userRef + ": role is missing.")
                        .hint("Assign a role that maps to basicAuth middleware.")
                        .example("role: admin")
                        .doc("middleware/basic-auth")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void noServicesConfigured() {
        final String msg = "No services configured.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("You must define at least one HTTP service.")
                        .example("""
                                services:
                                  - name: userApi
                                    url: http://localhost:30001
                                    methods: ['GET', 'POST']
                                    healthcheck: /ping
                                """)
                .doc("routing/services")
                .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingServiceName() {
        final String msg = "Service name is missing.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each service must have a unique name.")
                        .example("name: my-service")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void duplicateServiceName(String name) {
        final String msg = "Duplicate service name found: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Service names must be unique across the config.")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void missingServiceUrl(String name) {
        final String msg = "Service URL is missing for service: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each service must define a valid URL.")
                        .example("url: http://localhost:8080")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidServiceUrl(String url) {
        final String msg = "Service URL must start with 'http://' or 'https://': " + url;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void emptyHttpMethods(String name) {
        final String msg = "HTTP methods cannot be empty for service: " + name;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("HTTP methods cannot be empty for service: " + name)
                        .hint("List the allowed HTTP methods like GET, POST, etc.")
                        .example("methods: ['GET', 'POST']")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidHttpMethod(String method, String serviceName) {
        final String msg = "Invalid HTTP method '" + method + "' for service: " + serviceName;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Allowed methods are GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE, CONNECT.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void invalidHealthcheckPath(String path) {
        final String msg = "Healthcheck path must start with '/': " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void noProxiesConfigured() {
        final String msg = "No proxies configured.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg
                        )
                        .hint("At least one proxy route is required to forward traffic.")
                        .example("proxies:\n  - path: /api\n    service: apiService")
                        .doc("routing/routers")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void noServicesForProxies() {
        final String msg = "No services configured (HTTP or gRPC), but proxies depend on them.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Ensure that either HTTP services or gRPC services are defined.")
                        .doc("routing/services")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
    public static void proxyPathMissing() {
        final String msg = "Proxy path cannot be null or empty.";
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Proxy path cannot be null or empty.")
                        .hint("Each proxy must define a valid route path.")
                        .example("path: /users")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void proxyServiceMissing(String path) {
        final String msg = "Proxy service cannot be null or empty for path: " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Each proxy must link to a defined service.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void proxyServiceUnregistered(String service, String path) {
        final String msg = "Proxy references an unregistered service: " + service + " for path: " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .hint("Ensure the service name is defined in the services or grpcServices block.")
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }

    public static void proxyPathMustStartWithSlash(String path) {
        final String msg = "Proxy path must start with '/': " + path;
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error(msg)
                        .build()
        );
        throw new JetProxyValidationException(msg);
    }
}
