package proxy.middleware.log;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppContext;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AccessLog implements RequestLog {
    private static final Logger logger = LoggerFactory.getLogger(AccessLog.class);

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m"; // Success (2xx)
    private static final String YELLOW = "\u001B[33m"; // Redirects (3xx)
    private static final String RED = "\u001B[31m"; // Errors (4xx and 5xx)
    private static final String CYAN = "\u001B[36m"; // Informational (1xx)

    @Override
    public void log(Request request, Response response) {
        if (!AppContext.get().getConfig().isDebugMode())
            return;

        String remoteAddr = request.getRemoteAddr();
        String timestamp = dateFormatter.format(new Date());
        String method = request.getMethod();
        String path = request.getRequestURI();
        String protocol = request.getProtocol();
        int status = response.getStatus();
        long contentLength = response.getHttpChannel().getBytesWritten();
        String referrer = request.getHeader("Referer") != null ? request.getHeader("Referer") : "-";
        String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "-";
        long requestDuration = System.currentTimeMillis() - request.getTimeStamp();
        String routerName = request.getAttribute("routerName") != null ? request.getAttribute("routerName").toString() : "-";
        String serverUrl = request.getAttribute("serverUrl") != null ? request.getAttribute("serverUrl").toString() : "-";

        // Extract query parameters
        String queryParams = request.getQueryString() != null ? request.getQueryString() : "-";

        // Check if the response was served from cache
        String cacheIndicator = response.getHeader("X-JetProxy-Cache") != null ? response.getHeader("X-JetProxy-Cache") : "false";

        // Determine the color based on the HTTP status code
        String color;
        if (status >= 200 && status < 300) {
            color = GREEN; // 2xx
        } else if (status >= 300 && status < 400) {
            color = YELLOW; // 3xx
        } else if (status >= 400 && status < 600) {
            color = RED; // 4xx and 5xx
        } else if (status >= 100 && status < 200) {
            color = CYAN; // 1xx
        } else {
            color = RESET; // Default
        }

        String logMessage = String.format(
                "%s [%s] \"%s %s %s\" %s%d%s %d \"%s\" \"%s\" \"%s\" \"%s\" %dms QueryParams: [%s] Cache: [%s]",
                remoteAddr, timestamp, method, path, protocol,
                color, status, RESET, contentLength, referrer, userAgent,
                routerName, serverUrl, requestDuration, queryParams, cacheIndicator
        );

        // Log the message
        logger.info(logMessage);
    }
}
