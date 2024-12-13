package io.jetproxy.logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppContext;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DebugAwareLogger {

    private final Logger logger;
    private final boolean debugMode;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m"; // Success (2xx)
    private static final String YELLOW = "\u001B[33m"; // Redirects (3xx)
    private static final String RED = "\u001B[31m"; // Errors (4xx and 5xx)
    private static final String CYAN = "\u001B[36m"; // Informational (1xx)

    private DebugAwareLogger(Class<?> clazz, boolean debugMode) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.debugMode = debugMode;
    }

    public static DebugAwareLogger getLogger(Class<?> clazz) {
        return new DebugAwareLogger(clazz, AppContext.get().isDebugMode());
    }

    public void debug(String message, Object... args) {
        if (debugMode) {
            logger.info(message, args);
        } else {
            logger.debug(message, args);
        }
    }

    public void logRequest(Request request, Response response) {
        if (!debugMode) return;

        String logMessage = formatLogMessage(
                request,
                response,
                null, // Target URL is not applicable for general requests
                response.getStatus(),
                System.currentTimeMillis() - request.getTimeStamp(),
                response.getHeader("X-JetProxy-Cache") != null ? "true" : "false",
                "Request processed"
        );
        logger.info(logMessage);
    }

    public void logAuth(Request request, String targetUrl, int responseCode, long startTime, String status) {
        if (!debugMode) return;

        // Extract target URL and response details
        long responseTime = System.currentTimeMillis() - startTime;

        String logMessage = formatLogMessage(
                request,
                null, // Response is not directly applicable for auth logs
                targetUrl,
                responseCode,
                responseTime,
                "-", // Cache indicator is not applicable for auth logs
                status
        );
        logger.info(logMessage);
    }

    private String formatLogMessage(Request request, Response response, String targetUrl,
                                    int responseCode, long responseTime, String cacheIndicator, String status) {
        // Extract details from the request
        String remoteAddr = request.getRemoteAddr();
        String clientUserName = request.getRemoteUser() != null ? request.getRemoteUser() : "-";
        String timestamp = dateFormatter.format(new Date());
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryParams = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String protocol = request.getProtocol();
        String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "-";
        String referrer = request.getHeader("Referer") != null ? request.getHeader("Referer") : "-";

        // Extract details from the response
        String responseDetails = response != null ? String.valueOf(response.getHttpChannel().getBytesWritten()) : "-";
        String color = getStatusColor(responseCode);

        // Format the log entry
        return String.format(
                "%s - %s [%s] \"%s %s%s %s\" %s%d%s %s \"%s\" \"%s\" [%dms] Cache: [%s] Status: [%s]",
                remoteAddr, clientUserName, timestamp, method, path, queryParams, protocol,
                color, responseCode, RESET, responseDetails, referrer, userAgent, responseTime, cacheIndicator, status
        );
    }


    private String getStatusColor(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return GREEN;
        } else if (statusCode >= 300 && statusCode < 400) {
            return YELLOW;
        } else if (statusCode >= 400 && statusCode < 600) {
            return RED;
        } else if (statusCode >= 100 && statusCode < 200) {
            return CYAN;
        }
        return RESET;
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void error(String message, Object... args) {
        logger.error(message, args);
    }
}
