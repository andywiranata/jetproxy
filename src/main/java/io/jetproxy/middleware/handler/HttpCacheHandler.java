package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.CacheFactory;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.util.Constants;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class HttpCacheHandler  implements MiddlewareHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response)  {
        ResponseCacheEntry cachedResponse = getCachedResponse(request);
        if (cachedResponse == null) {
            return;
        }
        try {
            for (Map.Entry<String, String> header : cachedResponse.getHeaders().entrySet()) {
                response.setHeader(header.getKey(), header.getValue());
            }
            response.setHeader(Constants.HEADER_X_JETPROXY_CACHE, "true");
            response.getWriter().write(cachedResponse.getBody());
            response.getWriter().flush();
            response.flushBuffer();
        } catch (IOException ignored) {}

    }
    protected ResponseCacheEntry getCachedResponse(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        AppContext ctx = AppContext.get();
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        String responseBody = ctx.getCache().get(String.format(CacheFactory.HTTP_REQUEST_CACHE_KEY, method, path));

        return ctx.gson.fromJson(responseBody, ResponseCacheEntry.class);
    }
}
