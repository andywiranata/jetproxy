package proxy.service.holder;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.util.Callback;
import proxy.context.AppConfig;
import proxy.context.AppContext;
import proxy.logger.DebugAwareLogger;
import proxy.middleware.circuitbreaker.CircuitBreakerFactory;
import proxy.middleware.rule.RuleFactory;
import proxy.middleware.rule.header.HeaderAction;
import proxy.middleware.rule.header.HeaderActionFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ProxyHolder extends AbstractProxyHandler {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ProxyHolder.class);

    public ProxyHolder(AppConfig.Service configService,
                       AppConfig.Proxy proxyRule) {
        this.configService = configService;
        this.proxyRule = proxyRule;
        AppConfig.Middleware middleware = proxyRule.getMiddleware();
        // Initialize the ruleContext and circuitBreakerUtil
        this.ruleContext = Optional.ofNullable(proxyRule.getMiddleware())
                .map(AppConfig.Middleware::getRule)
                .map(RuleFactory::createRulesFromString)
                .orElse(null);

        this.circuitBreakerUtil = Optional.ofNullable(proxyRule.getMiddleware())
                .filter(AppConfig.Middleware::hasCircuitBreaker)
                .map(AppConfig.Middleware::getCircuitBreaker)
                .filter(AppConfig.CircuitBreaker::isEnabled)
                .map(cbConfig -> CircuitBreakerFactory.createCircuitBreaker("cb::" + proxyRule.getPath(), cbConfig))
                .orElse(null);

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
            if (AppContext.get().isDebugMode()) {
                // Collect headers into a single formatted string
                String headersLog = Collections.list(request.getHeaderNames()).stream()
                        .map(header -> header + ": " + request.getHeader(header))
                        .collect(Collectors.joining(", "));

                // Collect query parameters into a single formatted string
                String paramsLog = request.getParameterMap().entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                        .collect(Collectors.joining(", "));
                // Combine all details into a single log entry
                logger.debug("Incoming request: Method={}, URI={}, IP={}, Headers=[{}], Query Parameters=[{}]",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getRemoteAddr(),
                        headersLog,
                        paramsLog);
            }

            if (hasRuleContext() && !ruleContext.evaluate(request)) {
                logger.debug("Request denied by rule evaluation. URI: {}, Method: {}, IP: {}",
                        request.getRequestURI(), request.getMethod(), request.getRemoteAddr());
                handleRuleNotAllowed(response);
                return;
            }

            if (isMethodNotAllowed(request)) {
                logger.debug("Request denied due to disallowed HTTP method. URI: {}, Method: {}, IP: {}",
                        request.getRequestURI(), request.getMethod(), request.getRemoteAddr());
                handleMethodNotAllowed(response);
                return;
            }

            String cachedResponse = getCachedResponse(request);
            if (cachedResponse != null) {
                logger.debug("Serving cached response for URI: {}. Method: {}, IP: {}",
                        request.getRequestURI(), request.getMethod(), request.getRemoteAddr());
                sendCachedResponse(response, cachedResponse);
                return;
            }

            if (hasCircuitBreaker() &&
                     circuitBreakerUtil.getCircuitBreaker().getState() == CircuitBreaker.State.OPEN) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }

            try {
                HttpServletRequestWrapper modifiedRequest = modifyRequestHeaders(request);
                if (hasCircuitBreaker()) {
                    // Execute the service request within the circuit breaker
                    circuitBreakerUtil.executeRunnableWithCircuitBreaker(() -> {
                        try {
                            super.service(modifiedRequest, response);
                        } catch (Exception e) {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            throw new RuntimeException("Service failed", e);
                        }
                    });
                } else {
                    // If no circuit breaker, proceed without it
                    super.service(modifiedRequest, response);
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } finally {
            this.metricsListener.captureMetricProxyResponse(request, response);
            logger.debug("Proxy from -> {} -> to {}", request.getRequestURI()
                    , this.configService.getUrl());

        }
    }

    @Override
    protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
        // Extract existing headers from the server response
        Map<String, String> serverHeaders = serverResponse.getHeaders().stream()
                .collect(Collectors.toMap(
                        header -> header.getName(),
                        header -> header.getValue()
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

    }

    @Override
    protected void onProxyResponseFailure(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse, Throwable failure) {
        String path = clientRequest.getRequestURI();
        logger.error("Failed proxy from -> {} -> to {}", path, this.configService.getUrl());
        super.onProxyResponseFailure(clientRequest, proxyResponse, serverResponse, failure);

    }
    @Override
    protected void onResponseContent(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Response proxyResponse,
                                     byte[] buffer,
                                     int offset,
                                     int length, Callback callback) {
        // TODO Only support http
        if (this.proxyRule.getTtl() > 0) {
            String contentType = proxyResponse.getHeaders().get("Content-Type");
            String contentEncoding = proxyResponse.getHeaders().get("Content-Encoding");
            try (InputStream decodedStream = decodeContentStream(new ByteArrayInputStream(buffer, offset, length), contentEncoding)) {
                if (isJsonContent(contentType)) {
                    String bodyContent = readStreamAsString(decodedStream, contentType);
                    cacheResponseContent(request, bodyContent);
                }
            } catch (IOException e) {
                logger.error("Error decode response content {}", e.getMessage());
            }
        }

        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
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
