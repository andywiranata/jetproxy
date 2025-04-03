package io.jetproxy.middleware.handler;

import com.google.gson.Gson;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.Cache;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class HttpCacheHandlerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Cache mockCache;
    private AppContext ctx;
    private Gson gson;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        mockCache = mock(Cache.class);
        ctx = mock(AppContext.class);
        gson = new Gson();

        when(ctx.getCache()).thenReturn(mockCache);
        when(ctx.getGson()).thenReturn(gson);
    }

    @Test
    void should_respond_with_cached_response_if_exists() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/product");

        ResponseCacheEntry entry = new ResponseCacheEntry(
                Map.of("X-Cached", "yes"),
                "{\"product\":\"apple\"}"
        );
        String cacheKey = "http_request::GET:/product:"; // from CacheFactory.HTTP_REQUEST_CACHE_KEY

        when(mockCache.get(cacheKey)).thenReturn(gson.toJson(entry));

        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        HttpCacheHandler handler = new HttpCacheHandler(ctx);
        handler.handle(request, response);

        verify(response).setStatus(200);
        verify(response).setHeader("X-Cached", "yes");
        verify(response).setHeader("X-JetProxy-Cache", "true");
        assertTrue(output.toString().contains("\"product\":\"apple\""));
    }

    @Test
    void should_skip_if_method_not_supported() {
        when(request.getMethod()).thenReturn("POST");

        HttpCacheHandler handler = new HttpCacheHandler(ctx);
        handler.handle(request, response);

        verifyNoInteractions(response);
    }

    @Test
    void should_do_nothing_if_cache_miss() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/product");
        when(mockCache.get(any())).thenReturn(null);

        HttpCacheHandler handler = new HttpCacheHandler(ctx);
        handler.handle(request, response);

        verify(response, never()).setStatus(anyInt());
    }
}
