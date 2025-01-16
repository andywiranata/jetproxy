package io.jetproxy.service.holder;

import io.jetproxy.exception.ResilienceCircuitBreakerException;
import io.jetproxy.exception.ResilienceRateLimitException;
import io.jetproxy.middleware.handler.MatchServiceHandler;
import io.jetproxy.middleware.resilience.ResilienceFactory;
import io.jetproxy.middleware.handler.MiddlewareChain;
import io.jetproxy.util.Constants;
import io.jetproxy.util.CustomHttpServletRequestWrapper;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.Callback;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.rule.header.HeaderActionFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static io.jetproxy.util.Constants.REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE;

public class ProxyRequestHandler extends BaseProxyRequestHandler {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ProxyRequestHandler.class);
    private final MiddlewareChain middlewareChain;

    public ProxyRequestHandler(AppConfig.Service configService,
                               AppConfig.Proxy proxyRule,
                               MiddlewareChain middlewareChain) {
        this.configService = configService;
        this.proxyRule = proxyRule;
        this.middlewareChain = middlewareChain;
        this.resilience = ResilienceFactory.createResilienceUtil(proxyRule);
        // this.metricsListener = AppContext.get().getMetricsListener();
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

        logger.info("ProxyHolder Initialization ProxyID:{} - Rule: Path={} -> {}, Details={}",
                proxyRule.getUuid(), proxyRule.getPath(),
                AppContext.get().getServiceMap().get(proxyRule.getService()).getUrl(),
                proxyRule);
    }

    @Override
    protected String rewriteTarget(HttpServletRequest request) {
        // Get the rewritten target URI from the superclass
        String target = super.rewriteTarget(request);
        // Retrieve the matched service name from the request attribute
        String serviceName = (String) request.getAttribute(REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE);
        // Fetch the corresponding service configuration
        AppConfig.Service service = AppContext.get().getServiceMap().get(serviceName);
        // If a matching service is found, construct the new target URL
        if (service != null) {
            String serviceUrl = service.getUrl();
            String pathWithQuery = RequestUtils.extractPathWithQuery(target);
            return serviceUrl + pathWithQuery;
        }
        // Default to the original target if no service matched
        return target;
    }
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (middlewareChain != null) {
                middlewareChain.process(request, response);
                if (response.isCommitted()) {
                    return;
                }
            }
            this.resilience.execute(()-> {
                try {
                    HttpServletRequestWrapper httpServletRequestWrapper = this.modifyRequestHeaders(request);
                    super.service(httpServletRequestWrapper, response);
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
        }
    }

    @Override
    protected void sendProxyRequest(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Request proxyRequest) {
        clientRequest.setAttribute("startTime", System.nanoTime());

        super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);

        if (clientRequest.getAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING) != null) {
            String mirroringService = (String) clientRequest.getAttribute(
                    Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING);
            AppConfig.Service service = AppContext.get().getServiceMap().get(mirroringService);
            if (service != null) {
                sendMirrorProxyRequest(
                        this.rewriteTarget(clientRequest),
                        proxyRequest,
                        service);
            }

        }
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
        logger.error("Proxy response failure. Client request URI: {}, Server response status: {}, Error: {}",
                clientRequest.getRequestURI(),
                status,
                failure.getMessage());
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
    private void sendMirrorProxyRequest(String target,
                                        Request request,
                                        AppConfig.Service service) {
        try {
            HttpClient httpClient = getHttpClient();
            String pathWithQuery = RequestUtils.extractPathWithQuery(target);
            String mirrorServiceUri = service.getUrl() + pathWithQuery;

            httpClient.newRequest(mirrorServiceUri)
                    .method(request.getMethod())
                    .headers(mutable -> {
                        Enumeration<String> headerNames = request.getHeaders().getFieldNames();
                        while (headerNames.hasMoreElements()) {
                            String headerName = headerNames.nextElement();
                            String headerValue = request.getHeaders().get(headerName);
                            mutable.put(headerName, headerValue);
                        }
                    })
                    .send(result -> {
                        if (result.isFailed()) {
                            logger.error("Failed to mirror request to: {}", mirrorServiceUri, result.getFailure());
                        } else {
                            logger.info("Successfully mirrored request to: {}", mirrorServiceUri);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error mirroring request", e);
        }
    }
}
