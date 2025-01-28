package io.jetproxy.service.holder;

import io.jetproxy.exception.ResilienceCircuitBreakerException;
import io.jetproxy.exception.ResilienceRateLimitException;
import io.jetproxy.middleware.resilience.ResilienceFactory;
import io.jetproxy.middleware.handler.MiddlewareChain;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.Callback;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.rule.header.HeaderActionFactory;

import java.io.*;
import java.util.*;

public class ProxyRequestHandler extends BaseProxyRequestHandler {
    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(ProxyRequestHandler.class);
    private final MiddlewareChain middlewareChain;

    public ProxyRequestHandler(AppConfig.Proxy proxyRule,
                               MiddlewareChain middlewareChain) {
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

        logger.info("ProxyHolder Initialization ProxyID:{} - Rule: Path={}, Details={}",
                proxyRule.getUuid(), proxyRule.getPath(),
                proxyRule);
    }

    @Override
    protected String rewriteTarget(HttpServletRequest request) {
        String target = super.rewriteTarget(request); // Original target
        return RequestUtils.rewriteTarget(request, target);
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
            /*
            String jsonRequest = "{ \"name\": \"Alice\", \"email\": \"alice@example.com\" }";
            String serviceName = "userservice.UserService";
            String methodName = "CreateUser";
            String fullMethodName = serviceName + "/" + methodName;

            GrpcChannelManager manager = GrpcChannelManager.getInstance();
            ManagedChannel channel = GrpcChannelManager.getInstance().getGrpcChannel("userApi");

            Descriptors.ServiceDescriptor serviceDescriptor = manager.fetchServiceDescriptor(channel, serviceName);
            Descriptors.MethodDescriptor methodDescriptor = serviceDescriptor.findMethodByName(methodName);
            DynamicMessage grpcRequest = manager.buildGrpcRequest(jsonRequest, methodDescriptor.getInputType());

            // Invoke the gRPC method dynamically
            DynamicMessage response1 = manager.invokeGrpcMethod(fullMethodName, grpcRequest, channel);
            String jsonResponse = manager.convertGrpcResponseToJson(response1);

            System.out.println(jsonResponse);
            */

            this.resilience.execute(()-> {
                try {
                    HttpServletRequestWrapper httpServletRequestWrapper = this.modifyRequestHeaders(request);
                    super.service(httpServletRequestWrapper, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            logger.error("Error Occurred to process request {}", e.getMessage());
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
        try {
            // Check if mirroring is required
            Optional<AppConfig.Service> mirroringService = RequestUtils.getMirroringService(
                    clientRequest);
            super.sendProxyGrpcRequest(clientRequest, proxyResponse, proxyRequest);
            if (mirroringService.isPresent()) {
                super.sendProxyRequestWithMirroring(
                        clientRequest, proxyResponse, proxyRequest, mirroringService.get());
            } else {
                // No mirroring required, proceed as normal
                super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onServerResponseHeaders(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        super.onServerResponseHeaders(clientRequest, proxyResponse, serverResponse);
        this.modifyResponseHeaders(clientRequest, proxyResponse, serverResponse);
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
}
