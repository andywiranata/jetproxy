package io.jetproxy.util;


import io.jetproxy.context.AppConfig;
import jakarta.servlet.http.HttpServletRequest;

public class RequestUtils {
    /**
     * Extracts the path and query string from the given target URL.
     *
     * @param target the full target URL
     * @return the path and query string
     */
    public static String extractPathWithQuery(String target) {
        int pathStartIndex = target.indexOf("/", target.indexOf("://") + 3);
        return target.substring(pathStartIndex);
    }

    public static String getFullPath(HttpServletRequest request) {
        String uri = request.getRequestURI();        // e.g., /example
        String queryString = request.getQueryString(); // e.g., params=1

        if (queryString != null) {
            return uri + "?" + queryString;          // Combine the URI with the query string
        } else {
            return uri;                               // If there is no query string, just return the URI
        }
    }
    public static int parseMaxAge(String cacheControl) {
        if (cacheControl == null || cacheControl.isEmpty()) {
            return -1; // Default value when max-age is not provided
        }

        String[] directives = cacheControl.split(",");
        for (String directive : directives) {
            directive = directive.trim();
            if (directive.startsWith("max-age=")) {
                try {
                    return Integer.parseInt(directive.substring(8)); // Extract and parse max-age value
                } catch (NumberFormatException e) {
                    System.err.println("Invalid max-age value: " + directive);
                    return -1;
                }
            }
        }
        return -1; // Default value when max-age is not found
    }

    public static String rewriteRequest(String targetUrl, AppConfig.Service newService) {
        if (newService == null) {
            return null;
        }
        String serviceUrl = newService.getUrl();
        String pathWithQuery = RequestUtils.extractPathWithQuery(targetUrl);
        return serviceUrl + pathWithQuery;
    }
}
