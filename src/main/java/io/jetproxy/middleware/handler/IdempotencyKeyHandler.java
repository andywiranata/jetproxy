package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.CacheFactory;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.util.Constants;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
public class IdempotencyKeyHandler implements MiddlewareHandler {

    public static final List<String> SUPPORTED_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

    private final AppConfig.Idempotency idempotency;
    private final boolean isActiveIdempotency;
    private final AppContext ctx;

    public IdempotencyKeyHandler(AppConfig.Proxy proxyRule, AppContext ctx) {
        this.ctx = ctx;

        if (proxyRule.hasMiddleware() && proxyRule.getMiddleware().hasIdempotency()) {
            this.idempotency = proxyRule.getMiddleware().getIdempotency();
            this.isActiveIdempotency = true;
        } else {
            this.idempotency = null;
            this.isActiveIdempotency = false;
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getMethod().toUpperCase();
        if (!isActiveIdempotency || !SUPPORTED_METHODS.contains(method)) return;

        String idempotencyKey = request.getHeader(idempotency.getHeaderName());
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            sendError(response, 400, "Idempotency-Key header is missing");
            return;
        }

        String path = RequestUtils.getFullPath(request);
        String cacheKey = String.format(CacheFactory.HTTP_IDEMPOTENCY_KEY, method, path, idempotencyKey);

        String cachedJson = ctx.getCache().get(cacheKey);
        if (cachedJson != null) {
            ResponseCacheEntry cached = ctx.getGson().fromJson(cachedJson, ResponseCacheEntry.class);
            try {
                for (Map.Entry<String, String> header : cached.getHeaders().entrySet()) {
                    response.setHeader(header.getKey(), header.getValue());
                }
                response.setHeader(Constants.HEADER_X_JETPROXY_IDEMPOTENCY_CACHE, "true");
                response.setStatus(200);
                response.getWriter().write(cached.getBody());
                response.getWriter().flush();
                response.flushBuffer();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, Response proxyResponse, byte[] buffer) {
        MiddlewareHandler.super.postHandle(request, proxyResponse, buffer);
    }

    private void sendError(HttpServletResponse response, int code, String message) {
        try {
            response.setStatus(code);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + message + "\"}");
            response.getWriter().flush();
            response.flushBuffer();
        } catch (IOException ignored) {}
    }
}
