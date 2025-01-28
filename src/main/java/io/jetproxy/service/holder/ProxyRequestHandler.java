package io.jetproxy.service.holder;

import io.grpc.ManagedChannel;
import io.jetproxy.exception.ResilienceCircuitBreakerException;
import io.jetproxy.exception.ResilienceRateLimitException;
import io.jetproxy.middleware.grpc.GrpcChannelManager;
import io.jetproxy.middleware.resilience.ResilienceFactory;
import io.jetproxy.middleware.handler.MiddlewareChain;
import io.jetproxy.util.BufferedHttpServletRequestWrapper;
import io.jetproxy.util.Constants;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.Callback;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.rule.header.HeaderActionFactory;

import java.io.*;
import java.util.*;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;


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
        AppConfig.Service service = AppContext.get().getServiceMap().get(
                (String) request.getAttribute(REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE));
        String rewriteRequestUrl = RequestUtils.rewriteRequest(target, service);
        if (rewriteRequestUrl != null) {
            return rewriteRequestUrl;
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
        String mirroringService = (String) clientRequest.getAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING);
        AppConfig.Service service = AppContext.get().getServiceMap().get(mirroringService);
        if (service != null) {
            BufferedHttpServletRequestWrapper bufferedRequest = null;
            try {
                // Wrap the original request to buffer its content
                bufferedRequest = new BufferedHttpServletRequestWrapper(clientRequest);

                // Optionally, set the body to the proxyRequest
                if (!bufferedRequest.isEmptyBody()) {
                    proxyRequest.content(new BytesContentProvider(
                            bufferedRequest.getBodyAsByte()), bufferedRequest.getContentType());
                }
                // Forward the headers from the original request to the proxyRequest
                copyHeaders(bufferedRequest, proxyRequest);
                // Proceed with the proxy request
                super.sendProxyRequest(bufferedRequest, proxyResponse, proxyRequest);

                sendMirrorRequest(
                        RequestUtils.rewriteRequest(this.rewriteTarget(clientRequest), service),
                        clientRequest,
                        bufferedRequest);
            } catch (IOException e) {
                throw new RuntimeException("Error buffering request content", e);
            }
        } else {
            super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest);
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
