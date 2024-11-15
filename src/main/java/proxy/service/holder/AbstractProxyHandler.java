package proxy.service.holder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.context.AppConfig;
import proxy.context.AppContext;
import proxy.middleware.circuitbreaker.CircuitBreakerUtil;
import proxy.middleware.metric.MetricsListener;
import proxy.middleware.rule.RuleContext;
import proxy.middleware.rule.header.HeaderAction;
import proxy.util.RequestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public abstract class AbstractProxyHandler extends ProxyServlet.Transparent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProxyHandler.class);
    protected AppConfig.Service configService;
    protected AppConfig.Proxy proxyRule;
    protected RuleContext ruleContext;
    protected MetricsListener metricsListener;
    protected CircuitBreakerUtil circuitBreakerUtil;
    protected List<HeaderAction> headerRequestActions = Collections.emptyList();;
    protected List<HeaderAction> headerResponseActions;




    // Shared logic for checking the cache
    protected String getCachedResponse(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        return AppContext
                .get()
                .getCache()
                .get(String.format("%s__%s",
                        method, path));
    }

    // Shared logic for caching the response
    protected void cacheResponseContent(HttpServletRequest request, String bodyContent) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return;
        }
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        AppContext
                .get()
                .getCache()
                .put(String.format("%s__%s", method, path),
                        bodyContent,
                        proxyRule.getTtl());

    }

    // Shared logic for sending cached responses
    protected void sendCachedResponse(HttpServletResponse response, String cachedResponse) {
        response.setContentType("application/json");
        try {
            response.getWriter().write(cachedResponse);
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

    // Shared error handling logic
    protected void handleError(HttpServletResponse response, Exception e) {
        logger.error("Error processing request: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    protected void handleMethodNotAllowed(HttpServletResponse response) {
        logger.warn("Method not allowed processing request {}", this.configService.getName());
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void handleRuleNotAllowed(HttpServletResponse response) {
        logger.warn("Rules not allowed processing request {} {}", this.configService.getName(),
                this.proxyRule.getMiddleware().getRule());
        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    protected boolean hasCircuitBreaker() {
        return this.circuitBreakerUtil != null;
    }

    protected boolean hasRuleContext() {
        return this.ruleContext != null;
    }



}
