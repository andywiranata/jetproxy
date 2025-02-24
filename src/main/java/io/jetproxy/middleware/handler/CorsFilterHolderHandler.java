package io.jetproxy.middleware.handler;

import io.jetproxy.middleware.cors.CorsProcessor;
import io.jetproxy.context.AppConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorsFilterHolderHandler {
    private final AppConfig appConfig;

    /**
     * Constructor to initialize with application configuration.
     *
     * @param appConfig The application configuration containing CORS settings.
     */
    public CorsFilterHolderHandler(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Creates and configures a FilterHolder for CORS.
     *
     * @return A configured FilterHolder instance for CORS.
     */
    public CorsProcessor createCorsFilter() {
        // Retrieve CORS settings from AppConfig
        List<String> allowMethods = appConfig.getCorsFilter().getAccessControlAllowMethods();
        List<String> allowHeaders = appConfig.getCorsFilter().getAccessControlAllowHeaders();
        List<String> allowOrigins = appConfig.getCorsFilter().getAccessControlAllowOriginList();
        String maxAge = appConfig.getCorsFilter().getMaxAge();

        Set<String> methodSet = handleWildcard(allowMethods, "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        Set<String> headerSet = handleWildcard(allowHeaders, "*"); // "*" means all headers
        Set<String> originSet = handleWildcard(allowOrigins, "*");

        // Create and return CorsProcessor
        return new CorsProcessor(
                originSet,
                methodSet,
                headerSet,
                Set.of("*"),
                true,
                maxAge
        );
    }


    /**
     * Helper method to handle "*" (all) values.
     * If "*" is in the list, return a wildcard set or a default set based on the type.
     */
    private Set<String> handleWildcard(List<String> values, String... defaults) {
        if (values == null || values.isEmpty()) {
            return new HashSet<>();
        }
        if (values.contains("*")) {
            return defaults.length > 0 ? new HashSet<>(List.of(defaults)) : Collections.singleton("*");
        }
        return new HashSet<>(values);
    }
}
