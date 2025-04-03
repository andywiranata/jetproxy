package io.jetproxy.middleware.handler;

import com.google.gson.Gson;
import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.cache.Cache;
import io.jetproxy.middleware.cache.ResponseCacheEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class IdempotencyKeyHandlerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    Cache mockCache;
    private AppContext ctx;
    private Gson gson;

    private AppConfig.Proxy proxyRule;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        ctx = mock(AppContext.class); // ✅ must be mocked
        mockCache = mock(Cache.class);
        gson = new Gson();

        // Enable idempotency in config
        AppConfig.Idempotency idempotency = new AppConfig.Idempotency();
        idempotency.setHeaderName("Idempotency-Key");
        idempotency.setEnabled(true);

        AppConfig.Middleware middleware = new AppConfig.Middleware();
        middleware.setIdempotency(idempotency);

        proxyRule = new AppConfig.Proxy();
        proxyRule.setMiddleware(middleware);
        proxyRule.setPath("/upload");
        proxyRule.setService("fileUploadService");

        mockCache = mock(Cache.class);
        when(ctx.getCache()).thenReturn(mockCache); // stub getCache() first
        when(ctx.getGson()).thenReturn(new Gson());


    }

    @Test
    void should_return_400_when_idempotency_key_missing() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn(null);

        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        IdempotencyKeyHandler handler = new IdempotencyKeyHandler(proxyRule, ctx);
        handler.handle(request, response);

        verify(response).setStatus(400);
        assertTrue(output.toString().contains("Idempotency-Key header is missing"));
    }

    @Test
    void should_respond_with_cached_response_if_exists() throws Exception {

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn("abc123");
        when(request.getRequestURI()).thenReturn("/upload");
        ResponseCacheEntry entry = new ResponseCacheEntry(Map.of("X-From-Cache", "yes"), "{\"order\":1}");
        when(mockCache.get("idempotency:POST:/upload:abc123")).thenReturn(gson.toJson(entry));

        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        IdempotencyKeyHandler handler = new IdempotencyKeyHandler(proxyRule, ctx);
        handler.handle(request, response);

        verify(response).setStatus(200);
        verify(response).setHeader("X-From-Cache", "yes");
        verify(response).setHeader("X-JetProxy-Idempotency-Cache", "true");
        assertTrue(output.toString().contains("{\"order\":1}"));
    }

    @Test
    void should_not_modify_response_if_cache_miss() throws IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn("abc123");
        when(request.getRequestURI()).thenReturn("/upload");

        IdempotencyKeyHandler handler = new IdempotencyKeyHandler(proxyRule, ctx);
        handler.handle(request, response);

        verify(response, never()).setStatus(anyInt());
        verify(response, never()).getWriter();
    }

    @Test
    void should_skip_if_method_not_supported() {
        when(request.getMethod()).thenReturn("GET");

        IdempotencyKeyHandler handler = new IdempotencyKeyHandler(proxyRule, ctx);
        handler.handle(request, response);

        verifyNoInteractions(response);
    }

    @Test
    void should_skip_if_idempotency_disabled() {
        AppConfig.Proxy disabledProxy = new AppConfig.Proxy(); // no middleware
        IdempotencyKeyHandler handler = new IdempotencyKeyHandler(disabledProxy, ctx);

        when(request.getMethod()).thenReturn("POST");
        handler.handle(request, response);

        verifyNoInteractions(response);
    }
}
