package io.jetproxy.service.holder;

import io.jetproxy.exception.ResilienceCircuitBreakerException;
import io.jetproxy.exception.ResilienceRateLimitException;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.middleware.resilience.ResilienceFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.Callback;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.rule.RuleFactory;
import io.jetproxy.middleware.rule.header.HeaderActionFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ProxyRequestHandler extends BaseProxyHandler {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ProxyRequestHandler.class);

    public ProxyRequestHandler(AppConfig.Service configService,
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
            this.resilience.execute(()-> {
                try {
                    super.service(this.modifyRequestHeaders(request), response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            logger.debug("Error Occurred to process request {}", e.getMessage());
            if (e instanceof ResilienceRateLimitException) {
                sendRateLimiterResponse(response, e.getMessage());
            } else if (e instanceof ResilienceCircuitBreakerException) {
                AppConfig.CircuitBreaker circuitBreakerConfig = proxyRule.getMiddleware().getCircuitBreaker();
                sendServiceUnavailableResponse(response, circuitBreakerConfig.getRetryAfterSeconds(),
                        e.getMessage(), TYPE_CIRCUIT_BREAKER);
            } else {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
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
        Map<String, String> serverHeaders = extractHeadersFromServerResponse(serverResponse);
        // Apply response header actions
        Map<String, String> modifiedHeaders = applyResponseHeaderActions(serverHeaders);
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
}
