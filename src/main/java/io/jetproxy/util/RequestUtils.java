package io.jetproxy.util;


import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.service.holder.BaseProxyRequestHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

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
    public static boolean isJsonContent(String contentType) {
        return contentType != null && contentType.contains("application/json");
    }
    public static String rewriteRequest(String targetUrl, AppConfig.Service newService) {
        if (newService == null) {
            return null;
        }
        String serviceUrl = newService.getUrl();
        String pathWithQuery = RequestUtils.extractPathWithQuery(targetUrl);
        return serviceUrl + pathWithQuery;
    }
    /**
     * Retrieves a service from the request attribute and the service map.
     *
     * @param request       The HttpServletRequest object.
     * @return An Optional containing the AppConfig.Service if found, otherwise empty.
     */
    public static Optional<AppConfig.Service> getMirroringService(HttpServletRequest request) {
        String serviceName = (String) request.getAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING);
        if (serviceName == null || serviceName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(AppContext.get().getServiceMap().get(serviceName));
    }
    /**
     * Retrieves the service from the request attribute and rewrites the target URL.
     *
     * @param request       The HttpServletRequest object.
     * @param target        The original target URI.
     * @return The rewritten target URI, or the original target if no rewrite is needed.
     */
    public static String rewriteTarget(HttpServletRequest request, String target) {
        String serviceName = (String) request.getAttribute(
                Constants.REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE);
        if (serviceName == null || serviceName.isEmpty()) {
            return target; // Return original target if no service is found
        }
        AppConfig.Service service = AppContext.get().getServiceMap().get(serviceName);
        return (service != null) ? service.getUrl() + target : target; // Rewrite or return original
    }

    public static String getGrpcServiceName(HttpServletRequest request) {
        return (String) request.getAttribute(Constants
                .REQUEST_ATTRIBUTE_JETPROXY_GRPC_SERVICE_NAME);
    }
    public static String getGrpcMethodName(HttpServletRequest request) {
        return (String) request.getAttribute(Constants
                .REQUEST_ATTRIBUTE_JETPROXY_GRPC_METHOD_NAME);
    }
    public static boolean isProxyToGrpc(HttpServletRequest request) {
        return request.getAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_GRPC_SERVICE_NAME) != null &&
                request.getAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_GRPC_METHOD_NAME) != null;
    }
    public static void sendErrorResponse(HttpServletResponse response, int status, String error, String type) throws IOException {
        response.setStatus(status);
        response.setHeader(Constants.HEADER_X_PROXY_ERROR, error);
        response.setHeader(Constants.HEADER_X_PROXY_TYPE, type);
        response.flushBuffer();
    }

    public static void sendErrorServiceUnavailableResponse(HttpServletResponse response, int retryAfter, String errorMessage, String errorType) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader(Constants.HEADER_RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader(Constants.HEADER_X_PROXY_ERROR, errorMessage);
        response.setHeader(Constants.HEADER_X_PROXY_TYPE, errorType);
    }
    public static void sendErrorTooManyRequestsResponse(HttpServletResponse response, int retryAfter, String errorMessage, String errorType) {
        response.setStatus(429);
        response.setHeader(Constants.HEADER_RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader(Constants.HEADER_X_PROXY_ERROR, errorMessage);
        response.setHeader(Constants.HEADER_X_PROXY_TYPE, errorType);
    }
    public static void sendErrorRateLimiterResponse(HttpServletResponse response, String errorMessage) {
        response.setStatus(429);
        response.setHeader(Constants.HEADER_X_PROXY_ERROR, errorMessage);
        response.setHeader(Constants.HEADER_X_PROXY_TYPE, Constants.TYPE_RATE_LIMITER);
    }
}
