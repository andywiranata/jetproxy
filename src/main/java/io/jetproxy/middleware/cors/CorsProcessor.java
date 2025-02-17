package io.jetproxy.middleware.cors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CorsProcessor {

    // CORS Configuration
    private Set<String> allowedOrigins = new HashSet<>();
    private Set<String> allowedTimingOrigins = new HashSet<>();
    private Set<String> allowedMethods = new HashSet<>();
    private Set<String> allowedHeaders = new HashSet<>();
    private boolean allowCredentials = false;
    private String maxAge = "3600"; // Default cache time
    private boolean allowAllOrigins = false;
    private boolean allowAllMethods = false;
    private boolean allowAllHeaders = false;

    // Constructor for easy manual configuration (optional)
    public CorsProcessor(Set<String> allowedOrigins, Set<String> allowedMethods, Set<String> allowedHeaders,
                         Set<String> allowedTimingOrigins, boolean allowCredentials, String maxAge) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.allowedTimingOrigins = allowedTimingOrigins;
        this.allowCredentials = allowCredentials;
        this.maxAge = maxAge;
        this.allowAllOrigins = allowedOrigins.contains("*");
        this.allowAllMethods = allowedMethods.contains("*");
        this.allowAllHeaders = allowedHeaders.contains("*");
    }

    /**
     * Applies CORS headers to the response
     */
    public void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        if (origin != null) {
            if (allowAllOrigins && !allowCredentials) {
                response.setHeader("Access-Control-Allow-Origin", "*");
            } else if (allowAllOrigins) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            } else if (allowedOrigins.contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            }
        }

        if (allowCredentials) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        // Timing-Allow-Origin header
        if (allowedTimingOrigins.contains("*") || allowedTimingOrigins.contains(origin)) {
            response.setHeader("Timing-Allow-Origin", origin != null ? origin : "*");
        }

        if (allowAllMethods) {
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        } else {
            response.setHeader("Access-Control-Allow-Methods", String.join(",", allowedMethods));
        }

        if (allowAllHeaders) {
            response.setHeader("Access-Control-Allow-Headers", "*");
        } else {
            response.setHeader("Access-Control-Allow-Headers", String.join(",", allowedHeaders));
        }

        response.setHeader("Access-Control-Max-Age", maxAge);
    }

    /**
     * Handles pre-flight requests and returns true if handled
     */
    public boolean handlePreFlight(HttpServletRequest request, HttpServletResponse response) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            applyCorsHeaders(request, response);
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        return false;
    }
}
