package proxy.service.holder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;
import proxy.context.AppContext;
import proxy.middleware.cache.ResponseCacheEntry;
import proxy.middleware.metric.MetricsListener;
import proxy.middleware.resilience.ResilienceUtil;
import proxy.middleware.rule.RuleContext;
import proxy.middleware.rule.header.HeaderAction;
import proxy.util.RequestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public abstract class AbstractProxyHandler extends ProxyServlet.Transparent {
    // Header Names
    public static final String HEADER_RETRY_AFTER = "Retry-After";
    public static final String HEADER_X_PROXY_ERROR = "X-Proxy-Error";
    public static final String HEADER_X_PROXY_TYPE = "X-Proxy-Type";
    public static final String HEADER_X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_X_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    // Error Types
    public static final String TYPE_CIRCUIT_BREAKER = "circuit-breaker";
    public static final String TYPE_BULKHEAD = "bulkhead";
    public static final String TYPE_RATE_LIMITER = "rate-limiter";
    public static final String TYPE_METHOD_NOT_ALLOWED = "method-not-allowed";
    public static final String TYPE_RULE_NOT_ALLOWED = "rule-not-allowed";

    // Error Messages
    public static final String ERROR_METHOD_NOT_ALLOWED = "Method Not Allowed";
    public static final String ERROR_RULE_NOT_ALLOWED = "Rule not allowed processing request";

    public final String MODIFIED_HEADER = "modifiedHeader";
    private static final Logger logger = LoggerFactory.getLogger(AbstractProxyHandler.class);
    protected AppConfig.Service configService;
    protected AppConfig.Proxy proxyRule;
    protected RuleContext ruleContext;
    protected MetricsListener metricsListener;
    protected ResilienceUtil resilience;
    protected List<HeaderAction> headerRequestActions = Collections.emptyList();;
    protected List<HeaderAction> headerResponseActions;



    // Shared logic for checking the cache
    protected ResponseCacheEntry getCachedResponse(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        AppContext ctx = AppContext.get();
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        String responseBody = ctx.getCache().get(String.format("%s__%s", method, path));

        return ctx.gson.fromJson(responseBody, ResponseCacheEntry.class);

    }

    // Shared logic for caching the response
    protected void cacheResponseContent(HttpServletRequest request,
                                        String responseBody) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return;
        }
        AppContext ctx = AppContext.get();
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        ResponseCacheEntry cacheEntry = new ResponseCacheEntry(
                (Map<String, String>) request.getAttribute(MODIFIED_HEADER), responseBody);
        ctx.getCache()
                .put(String.format("%s__%s", method, path),
                        ctx.gson.toJson(cacheEntry),
                        proxyRule.getTtl());

    }

    // Shared logic for sending cached responses
    protected void sendCachedResponse(HttpServletResponse response, ResponseCacheEntry cachedResponse) {
        try {
            for (Map.Entry<String, String> header : cachedResponse.getHeaders().entrySet()) {
                response.setHeader(header.getKey(), header.getValue());
            }
            response.setHeader("X-JetProxy-Cache", "true");
            response.getWriter().write(cachedResponse.getBody());
            response.getWriter().flush();
        } catch (IOException e) {
            logger.error("Error writing cached response: {}", e.getMessage());
        }
    }

    // Shared logic for decoding content streams
    protected InputStream decodeContentStream(InputStream inputStream, String contentEncoding)
            throws IOException {
        if (contentEncoding == null) {
            return inputStream; // No encoding, return original stream
        }
        return switch (contentEncoding) {
            case "gzip" -> new GZIPInputStream(inputStream);
            case "deflate" -> new InflaterInputStream(inputStream);  // Handles ZLIB header
            case "br" -> new BrotliInputStream(inputStream);  // Handles Brotli encoding
            default -> inputStream; // Unknown encoding, return original stream
        };
    }

    // Shared logic for determining if content is JSON
    protected boolean isJsonContent(String contentType) {
        return contentType != null && contentType.contains("application/json");
    }

    // Shared logic for reading streams as a string
    protected String readStreamAsString(InputStream inputStream, String contentType) throws IOException {
        String charset = getCharsetFromContentType(contentType);
        return new String(inputStream.readAllBytes(), Charset.forName(charset));
    }

    // Extract charset from content type, defaulting to UTF-8
    protected String getCharsetFromContentType(String contentType) {
        if (contentType.contains("charset=")) {
            return contentType.split("charset=")[1];
        }
        return "UTF-8"; // Default to UTF-8 if no charset is specified
    }

    public boolean isMethodNotAllowed(HttpServletRequest request) {
        List<String> allowedMethods = this.configService.getMethods();
        String requestMethod = request.getMethod();
        return !allowedMethods.contains(requestMethod);
    }

    protected boolean handleAndSendResilienceResponse(HttpServletRequest request, HttpServletResponse response) {
        // CircuitBreaker logic
        if (this.resilience.isCircuitBreakerAllowRequest()) {
            AppConfig.CircuitBreaker circuitBreakerConfig = proxyRule.getMiddleware().getCircuitBreaker();
            int retryAfter = circuitBreakerConfig.getRetryAfterSeconds(); // Get Retry-After dynamically
            sendServiceUnavailableResponse(response, retryAfter,
                    "Circuit Breaker Open", TYPE_CIRCUIT_BREAKER);
            return true;
        }

        // Bulkhead logic
        /*
        if (this.resilience.isBulkheadAvailable()) {
            AppConfig.Bulkhead bulkheadConfig = proxyRule.getMiddleware().getBulkhead();
            int retryAfter = bulkheadConfig.getRetryAfterSeconds(); // Get Retry-After dynamically
            sendTooManyRequestsResponse(response, retryAfter,
                    "Bulkhead Limit Reached", TYPE_BULKHEAD);
            return true;
        }

        // RateLimiter logic
        if (this.resilience.isRateLimiterAvailable()) {
            AppConfig.RateLimiter rateLimiterConfig = proxyRule.getMiddleware().getRateLimiter();
            int resetAfter = rateLimiterConfig.getResetAfterSeconds(); // Get reset time dynamically
            int limit = rateLimiterConfig.getLimitForPeriod();
            int remaining = 0; // Assuming no remaining requests available
            sendRateLimiterResponse(response, resetAfter, limit, remaining,
                    "Rate Limiter Exceeded");
            return true;
        }
        */

        return false; // No resilience issues
    }

    protected void sendServiceUnavailableResponse(HttpServletResponse response, int retryAfter, String errorMessage, String errorType) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader(HEADER_X_PROXY_ERROR, errorMessage);
        response.setHeader(HEADER_X_PROXY_TYPE, errorType);
    }

    protected void sendTooManyRequestsResponse(HttpServletResponse response, int retryAfter, String errorMessage, String errorType) {
        response.setStatus(429);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader(HEADER_X_PROXY_ERROR, errorMessage);
        response.setHeader(HEADER_X_PROXY_TYPE, errorType);
    }

    protected void sendRateLimiterResponse(HttpServletResponse response, int resetAfter, int limit, int remaining, String errorMessage) {
        response.setStatus(429);
        response.setHeader(HEADER_X_RATE_LIMIT_LIMIT, String.valueOf(limit));
        response.setHeader(HEADER_X_RATE_LIMIT_REMAINING, String.valueOf(remaining));
        response.setHeader(HEADER_X_RATE_LIMIT_RESET, String.valueOf(resetAfter));
        response.setHeader(HEADER_X_PROXY_ERROR, errorMessage);
        response.setHeader(HEADER_X_PROXY_TYPE, TYPE_RATE_LIMITER);
    }

    protected void sendMethodNotAllowedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader(HEADER_X_PROXY_ERROR, ERROR_METHOD_NOT_ALLOWED);
        response.setHeader(HEADER_X_PROXY_TYPE, TYPE_METHOD_NOT_ALLOWED);
    }

    protected void sendRuleNotAllowedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        response.setHeader(HEADER_X_PROXY_ERROR, ERROR_RULE_NOT_ALLOWED);
        response.setHeader(HEADER_X_PROXY_TYPE, TYPE_RULE_NOT_ALLOWED);
    }
    protected boolean hasRuleContext() {
        return this.ruleContext != null;
    }

    protected boolean isCacheActive() {
        return proxyRule.getTtl() > 0;
    }



}
