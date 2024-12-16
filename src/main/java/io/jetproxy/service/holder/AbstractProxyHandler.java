package io.jetproxy.service.holder;

import io.jetproxy.context.AppConfig;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.middleware.rule.RuleContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.metric.MetricsListener;
import io.jetproxy.middleware.resilience.ResilienceUtil;
import io.jetproxy.middleware.rule.header.HeaderAction;
import io.jetproxy.util.RequestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
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
    protected void sendRateLimiterResponse(HttpServletResponse response, String errorMessage) {
        response.setStatus(429);
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


    protected Map<String, String> extractHeadersFromRequest(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        header -> header,
                        request::getHeader
                ));
    }
    protected void applyHeaderActions(HttpServletRequest request, Map<String, String> headers) {
        for (HeaderAction action : headerRequestActions) {
            action.execute(request, headers);
        }
    }
    protected HttpServletRequestWrapper modifyRequestHeaders(HttpServletRequest request) {
        // Create a mutable map for headers
        Map<String, String> modifiedHeaders = extractHeadersFromRequest(request);

        // Apply request header actions
        applyHeaderActions(request, modifiedHeaders);
        // Wrap the request with the modified headers
        return new HttpServletRequestWrapper(request) {
            @Override
            public Enumeration<String> getHeaderNames() {
                return Collections.enumeration(modifiedHeaders.keySet());
            }

            @Override
            public String getHeader(String name) {
                return modifiedHeaders.get(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return Collections.enumeration(Collections.singleton(modifiedHeaders.get(name)));
            }
        };
    }

    protected Map<String, String> extractHeadersFromServerResponse(Response serverResponse) {
        return serverResponse.getHeaders().stream()
                .collect(Collectors.toMap(
                        HttpField::getName,
                        HttpField::getValue
                ));
    }

    protected Map<String, String> applyResponseHeaderActions(Map<String, String> serverHeaders) {
        Map<String, String> modifiedHeaders = new HashMap<>();
        for (HeaderAction action : headerResponseActions) {
            action.execute(serverHeaders, modifiedHeaders);
        }
        return modifiedHeaders;
    }
}
