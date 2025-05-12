package io.jetproxy.util;

/**
 * Centralized fatal error hints for configuration validation in JetProxy.
 */
public class FatalValidationHints {

    public static void missingAppName() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Missing 'appName' in config.")
                        .hint("Set a unique name for your JetProxy app.")
                        .example("appName: jetproxy-gateway")
                        .build()
        );
    }

    public static void invalidPort(int actualPort) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid port number: " + actualPort)
                        .hint("Port must be in range 1–65535.")
                        .example("port: 8080")
                        .build()
        );
    }

    public static void invalidTimeout(int actualTimeout) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid default timeout: " + actualTimeout)
                        .hint("Set a positive timeout value in milliseconds.")
                        .example("defaultTimeout: 10000")
                        .build()
        );
    }

    public static void invalidRootPath(String path) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("Invalid rootPath: " + path)
                        .hint("It must start with '/'.")
                        .example("rootPath: /")
                        .build()
        );
    }

    public static void missingCorsFilter() {
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
    }

    public static void missingCorsMethods() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowMethods' is missing.")
                        .hint("Defines allowed HTTP methods for cross-origin requests.")
                        .example("accessControlAllowMethods: [*]")
                        .doc("middleware/cors")
                        .build()
        );
    }

    public static void missingCorsOrigins() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowOriginList' is missing.")
                        .hint("List domains allowed to make cross-origin requests.")
                        .example("accessControlAllowOriginList: ['https://example.com']")
                        .doc("middleware/cors")
                        .build()
        );
    }

    public static void missingCorsHeaders() {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("CORS: 'accessControlAllowHeaders' is missing.")
                        .hint("Specify headers that clients may include in requests.")
                        .example("accessControlAllowHeaders: ['Content-Type', 'Authorization']")
                        .doc("middleware/cors")
                        .build()
        );
    }

    public static void missingUsername(String userRef) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User config: username is missing.")
                        .hint("Each user must have a unique username.")
                        .example("username: userA")
                        .doc("middleware/basic-auth")
                        .build()
        );
    }

    public static void missingPassword(String userRef) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User " + userRef + ": password is missing.")
                        .hint("Define a plaintext password or use secure storage.")
                        .example("password: secret123")
                        .doc("middleware/basic-auth")
                        .build()
        );
    }

    public static void missingRole(String userRef) {
        JetProxyExit.fatal(
                JetProxyErrorBuilder.error("User " + userRef + ": role is missing.")
                        .hint("Assign a role that maps to basicAuth middleware.")
                        .example("role: admin")
                        .doc("middleware/basic-auth")
                        .build()
        );
    }
}
