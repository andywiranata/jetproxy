package io.jetproxy.service.holder;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.jetproxy.context.AppConfig;
import io.jetproxy.logger.DebugAwareLogger;
import io.jetproxy.middleware.cache.CacheFactory;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.middleware.grpc.GrpcChannelManager;
import io.jetproxy.middleware.grpc.GrpcToHttpStatusMapper;
import io.jetproxy.middleware.grpc.MockResponse;
import io.jetproxy.middleware.rule.RuleContext;
import io.jetproxy.util.BufferedHttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.brotli.dec.BrotliInputStream;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.resilience.ResilienceUtil;
import io.jetproxy.middleware.rule.header.HeaderAction;
import io.jetproxy.util.RequestUtils;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public abstract class BaseProxyRequestHandler extends ProxyServlet.Transparent {
    public final String RESPONSE_MODIFIED_HEADER = "modifiedHeader";
    public final String REQUEST_MODIFIED_HEADER = "requestModifiedHeader";

    private static final DebugAwareLogger logger = DebugAwareLogger.getLogger(DebugAwareLogger.class);
    protected AppConfig.Proxy proxyRule;
    protected RuleContext ruleContext;
    protected ResilienceUtil resilience;
    protected List<HeaderAction> headerRequestActions = Collections.emptyList();;
    protected List<HeaderAction> headerResponseActions;
    protected boolean isProxyToGrpc = false;

    // Shared logic for caching the response
    protected void cacheResponseContent(HttpServletRequest request,
                                        String responseBody,
                                        String randomKey,
                                        long ttl,
                                        String cacheKey) {
        AppContext ctx = AppContext.get();
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        ResponseCacheEntry cacheEntry = new ResponseCacheEntry(
                (Map<String, String>) request.getAttribute(RESPONSE_MODIFIED_HEADER), responseBody);
        ctx.getCache()
                .put(String.format(
                        cacheKey, method, path, randomKey),
                        ctx.gson.toJson(cacheEntry),
                        ttl);
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
    protected Map<String, String> extractAttributesFromRequest(HttpServletRequest request) {
        return Collections.list(request.getAttributeNames())
                .stream()
                .collect(Collectors.toMap(
                        attributeName -> attributeName,
                        attributeName -> String.valueOf(request.getAttribute(attributeName)) // Convert to String
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
        Map<String, String> modifiedAttributes = extractAttributesFromRequest(request);
        modifiedHeaders.putAll(modifiedAttributes);
        // Apply request header actions
        applyHeaderActions(request, modifiedHeaders);
        request.setAttribute(REQUEST_MODIFIED_HEADER, modifiedHeaders);
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
    protected void modifyResponseHeaders(HttpServletRequest clientRequest,
                                       HttpServletResponse proxyResponse,
                                       Response serverResponse) {
        // Extract existing headers from the server response
        Map<String, String> serverHeaders = extractHeadersFromServerResponse(serverResponse);

        // Apply response header actions
        Map<String, String> modifiedHeaders = applyResponseHeaderActions(serverHeaders);

        // Set modified headers to the proxy response
        for (Map.Entry<String, String> entry : modifiedHeaders.entrySet()) {
            proxyResponse.setHeader(entry.getKey(), entry.getValue());
        }

        // Update the client request attributes with modified headers
        serverHeaders.putAll(modifiedHeaders);
        clientRequest.setAttribute(RESPONSE_MODIFIED_HEADER, serverHeaders);
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
    protected void copyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {
        Enumeration<String> headerNames = clientRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = clientRequest.getHeader(headerName);
            if (headerValue != null) {
                proxyRequest.header(headerName, headerValue);
            }
        }
    }
    protected void sendProxyRequestWithMirroring(HttpServletRequest clientRequest, HttpServletResponse proxyResponse,
                                               Request proxyRequest, AppConfig.Service service) throws IOException {
        // Wrap the original request to buffer its content
        BufferedHttpServletRequestWrapper bufferedRequest = new BufferedHttpServletRequestWrapper(clientRequest);

        // Optionally, set the body to the proxyRequest
        if (!bufferedRequest.isEmptyBody()) {
            proxyRequest.content(new BytesContentProvider(
                    bufferedRequest.getBodyAsByte()), bufferedRequest.getContentType());
        }
        // Forward the headers from the original request to the proxyRequest
        copyHeaders(bufferedRequest, proxyRequest);

        // Proceed with the proxy request
        super.sendProxyRequest(bufferedRequest, proxyResponse, proxyRequest);

        // Handle the mirroring logic
        String mirrorTarget = RequestUtils.rewriteRequest(this.rewriteTarget(clientRequest), service);
        sendMirrorRequest(mirrorTarget, clientRequest, bufferedRequest);


    }
    protected void sendMirrorRequest(String mirrorServiceUrl, HttpServletRequest clientRequest,
                                     BufferedHttpServletRequestWrapper bufferedRequest) {
        // Get mirroring service details (e.g., from config)

        try {
            HttpClient httpClient = getHttpClient(); // Assuming you have an HttpClient instance

            // Create a new mirrored request
            Request mirrorRequest = httpClient.newRequest(mirrorServiceUrl)
                    .method(clientRequest.getMethod())
                    .headers(mutable -> {
                        Enumeration<String> headerNames = clientRequest.getHeaderNames();
                        while (headerNames.hasMoreElements()) {
                            String headerName = headerNames.nextElement();
                            String headerValue = clientRequest.getHeader(headerName);
                            mutable.put(headerName, headerValue);
                        }
                    });
            // Set the mirrored request body
            if (!bufferedRequest.isEmptyBody()) {
                mirrorRequest.content(new BytesContentProvider(
                        bufferedRequest.getBodyAsByte()), clientRequest.getContentType());
            }

            // Send the mirrored request asynchronously
            mirrorRequest.send(result -> {
                if (result.isFailed()) {
                    logger.debug("Failed to mirror request to: {}", mirrorServiceUrl, result.getFailure());
                } else {
                    logger.debug("Successfully mirrored request to: {}", mirrorServiceUrl);
                }
            });
        } catch (Exception e) {
            logger.error("Error mirroring request", e);
        }
    }
    protected void sendProxyGrpcRequest(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Request proxyRequest) throws Exception {
        try {
            // Read JSON Request
            BufferedHttpServletRequestWrapper bufferedRequest = new BufferedHttpServletRequestWrapper(clientRequest);
            String jsonRequest = bufferedRequest.getBodyAsString();

            // Extract gRPC Service & Method Names
            String serviceName = RequestUtils.getGrpcServiceName(clientRequest);
            String methodName = RequestUtils.getGrpcMethodName(clientRequest);
            String fullMethodName = serviceName + "/" + methodName;

            // Get gRPC Channel & Metadata
            GrpcChannelManager manager = GrpcChannelManager.getInstance();
            ManagedChannel channel = manager.getGrpcChannel(this.proxyRule.getService());
            Map<String, String> metadataMap = (Map<String, String>) clientRequest.getAttribute(REQUEST_MODIFIED_HEADER);

            // Fetch Service Descriptor
            Descriptors.ServiceDescriptor serviceDescriptor = manager.fetchServiceDescriptor(channel, serviceName);
            Descriptors.MethodDescriptor methodDescriptor = serviceDescriptor.findMethodByName(methodName);
            if (methodDescriptor == null) {
                throw new IllegalArgumentException("gRPC method not found: " + methodName);
            }

            // Build & Invoke gRPC Request
            DynamicMessage grpcRequest = manager.buildGrpcRequest(jsonRequest, methodDescriptor.getInputType());
            DynamicMessage grpcResponse = manager.invokeGrpcMethod(fullMethodName, grpcRequest, channel, metadataMap);

            // Convert gRPC Response to JSON
            String jsonResponse = manager.convertGrpcResponseToJson(grpcResponse);
            MockResponse mockResponse = MockResponse.createSuccessResponse(jsonResponse);

            // Send Headers & Content
            onServerResponseHeaders(clientRequest, proxyResponse, mockResponse);
            onResponseContent(clientRequest, proxyResponse, mockResponse,
                    jsonResponse.getBytes(StandardCharsets.UTF_8), 0, jsonResponse.length(), Callback.NOOP);

            // Call Success Handler
            onProxyResponseSuccess(clientRequest, proxyResponse, mockResponse);

        } catch (StatusRuntimeException e) {
            sendProxyResponseError(clientRequest, proxyResponse,
                    GrpcToHttpStatusMapper.mapGrpcStatusToHttp(e.getStatus().getCode())
            );
        } catch (Exception e) {
            logger.error("Error handling gRPC request: {}", e.getMessage());
            MockResponse mockResponse = MockResponse.createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
                    "Internal Server Error");
            onProxyResponseFailure(clientRequest, proxyResponse, mockResponse, e);
        }
    }


}