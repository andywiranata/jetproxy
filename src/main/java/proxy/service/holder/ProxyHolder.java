package proxy.service.holder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.Callback;
import proxy.context.AppConfig;
import proxy.context.AppContext;
import proxy.logger.DebugAwareLogger;
import proxy.middleware.cache.ResponseCacheEntry;
import proxy.middleware.resilience.*;
import proxy.middleware.rule.RuleFactory;
import proxy.middleware.rule.header.HeaderAction;
import proxy.middleware.rule.header.HeaderActionFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ProxyHolder extends AbstractProxyHandler {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ProxyHolder.class);

    public ProxyHolder(AppConfig.Service configService,
                       AppConfig.Proxy proxyRule) {
        this.configService = configService;
        this.proxyRule = proxyRule;
        // Initialize the ruleContext and circuitBreakerUtil
        this.ruleContext = Optional.ofNullable(proxyRule.getMiddleware())
                .map(AppConfig.Middleware::getRule)
                .map(RuleFactory::createRulesFromString)
                .orElse(null);
        this.resilience = ResilienceFactory.createResilienceUtil(proxyRule);
        this.metricsListener = AppContext.get().getMetricsListener();
        this.headerRequestActions = Optional.ofNullable(proxyRule.getMiddleware())
                .filter(AppConfig.Middleware::hasHeaders)
                .map(AppConfig.Middleware::getHeader)
                .map(AppConfig.Headers::getRequestHeaders)
                .map(HeaderActionFactory::createActions)
                .orElseGet(Collections::emptyList);

        this.headerResponseActions = Optional.ofNullable(proxyRule.getMiddleware())
                .filter(AppConfig.Middleware::hasHeaders)
                .map(AppConfig.Middleware::getHeader)
                .map(AppConfig.Headers::getResponseHeaders)
                .map(HeaderActionFactory::createActions)
                .orElseGet(Collections::emptyList);

        logger.debug("ProxyHolder initiated with Proxy Rules: {} - {}",
                proxyRule.getPath(), proxyRule.toString());
        logger.debug("ProxyHolder initiated with Service Name: {} - {} ",
                configService.getName(), configService.toString());

    }
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (hasRuleContext() && !ruleContext.evaluate(request)) {
                sendRuleNotAllowedResponse(response);
                return;
            }

            if (isMethodNotAllowed(request)) {
                sendMethodNotAllowedResponse(response);
                return;
            }
            ResponseCacheEntry cachedResponse = getCachedResponse(request);
            if (cachedResponse != null) {
                sendCachedResponse(response, cachedResponse);
                return;
            }
            // Check resilience state and handle response if necessary
            if (this.handleResilienceState(request, response)) {
                return; // Resilience state handled, no further processing
            }
            this.resilience.execute(()-> {
                try {
                    super.service(modifyRequestHeaders(request), response);
                } catch (Exception e) {
                    logger.debug("Error Occurred to process request {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            });

        } finally {
            this.metricsListener.captureMetricProxyResponse(request, response);
        }
    }

    @Override
    protected void sendProxyRequest(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Request proxyRequest) {
        clientRequest.setAttribute("startTime", System.nanoTime());
        super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);
    }

    @Override
    protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
        // Extract existing headers from the server response
        Map<String, String> serverHeaders = serverResponse.getHeaders().stream()
                .collect(Collectors.toMap(
                        HttpField::getName,
                        HttpField::getValue
                ));
        // Apply response header actions
        Map<String, String> modifiedHeaders = new HashMap<>();
        for (HeaderAction action : headerResponseActions) {
            action.execute(serverHeaders, modifiedHeaders);
        }

        // Set modified headers to the proxy response
        for (Map.Entry<String, String> entry : modifiedHeaders.entrySet()) {
            proxyResponse.setHeader(entry.getKey(), entry.getValue());
        }
        serverHeaders.putAll(modifiedHeaders);
        clientRequest.setAttribute(MODIFIED_HEADER,serverHeaders);
    }

    @Override
    protected void onProxyResponseSuccess(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        resilience.handleHttpResponse(clientRequest, serverResponse.getStatus(), null);
        super.onProxyResponseSuccess(clientRequest, proxyResponse, serverResponse);
    }

    @Override
    protected void onProxyResponseFailure(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse, Throwable failure) {
        int status = this.proxyResponseStatus(failure);
        resilience.handleHttpResponse(clientRequest, status, failure);
        super.onProxyResponseFailure(clientRequest, proxyResponse, serverResponse, failure);

    }
    @Override
    protected void onResponseContent(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Response proxyResponse,
                                     byte[] buffer,
                                     int offset,
                                     int length, Callback callback) {
        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
        if (isCacheActive()) {
            String contentType = proxyResponse.getHeaders().get(HttpHeader.CONTENT_TYPE);
            String contentEncoding = proxyResponse.getHeaders().get(HttpHeader.CONTENT_ENCODING);
            try (InputStream decodedStream = decodeContentStream(new ByteArrayInputStream(buffer, offset, length), contentEncoding)) {
                if (isJsonContent(contentType)) {
                    String bodyContent = readStreamAsString(decodedStream, contentType);
                    cacheResponseContent(request, bodyContent);
                }
            } catch (Exception e) {
                logger.error("Error decode response content {}", e.getMessage());
            }
        }

    }
    private HttpServletRequestWrapper modifyRequestHeaders(HttpServletRequest request) {
        // Create a mutable map for headers
        Map<String, String> modifiedHeaders = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(
                        header -> header,
                        request::getHeader
                ));

        // Apply request header actions
        for (HeaderAction action : headerRequestActions) {
            action.execute(request, modifiedHeaders);
        }

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
}
