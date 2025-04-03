package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.CacheFactory;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.util.Constants;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HttpCacheHandler implements MiddlewareHandler {

    public static final List<String> SUPPORTED_METHODS = List.of("GET");

    private final AppContext ctx;

    public HttpCacheHandler(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        ResponseCacheEntry cachedResponse = getCachedResponse(request);
        if (cachedResponse == null) {
            return;
        }

        try {
            for (Map.Entry<String, String> header : cachedResponse.getHeaders().entrySet()) {
                response.setHeader(header.getKey(), header.getValue());
            }
            response.setHeader(Constants.HEADER_X_JETPROXY_CACHE, "true");
            response.setStatus(200);
            response.getWriter().write(cachedResponse.getBody());
            response.getWriter().flush();
            response.flushBuffer();
        } catch (IOException ignored) {}
    }

    protected ResponseCacheEntry getCachedResponse(HttpServletRequest request) {
        if (!SUPPORTED_METHODS.contains(request.getMethod().toUpperCase())) {
            return null;
        }

        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        String cacheKey = String.format(CacheFactory.HTTP_REQUEST_CACHE_KEY, method, path, "");

        String responseBody = ctx.getCache().get(cacheKey);
        if (responseBody == null) {
            return null;
        }

        return ctx.getGson().fromJson(responseBody, ResponseCacheEntry.class);
    }
}
