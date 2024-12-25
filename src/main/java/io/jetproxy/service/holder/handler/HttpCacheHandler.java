package io.jetproxy.service.holder.handler;

import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import io.jetproxy.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

import static io.jetproxy.service.holder.BaseProxyRequestHandler.HEADER_X_JETPROXY_CACHE;

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
            response.setHeader(HEADER_X_JETPROXY_CACHE, "true");
            response.getWriter().write(cachedResponse.getBody());
            response.getWriter().flush();
            response.flushBuffer();
        } catch (IOException e) {}

    }
    protected ResponseCacheEntry getCachedResponse(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            return null;
        }
        AppContext ctx = AppContext.get();
        String path = RequestUtils.getFullPath(request);
        String method = request.getMethod();
        String responseBody = ctx.getCache().get(String.format("%s__%s", method, path));

        return ctx.gson.fromJson(responseBody, ResponseCacheEntry.class);
    }
}
