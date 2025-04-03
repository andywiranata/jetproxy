package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class MirroringHandlerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private AppConfig.Proxy proxyRule;
    private AppConfig.Middleware middleware;
    private AppConfig.Mirroring mirroring;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        mirroring = new AppConfig.Mirroring();
        mirroring.setMirrorService("mirrorServiceA");

        middleware = new AppConfig.Middleware();
        middleware.setMirroring(mirroring);

        proxyRule = new AppConfig.Proxy();
        proxyRule.setMiddleware(middleware);
    }

    @Test
    void should_mirror_request_if_percentage_matches_user_id() {
        when(request.getHeader(Constants.REQUEST_HEADER_USER_ID)).thenReturn("user123");

        // Simulate hash value < 100 to always match
        mirroring.setMirrorPercentage(100);
        mirroring.setEnabled(true);

        MirroringHandler handler = new MirroringHandler(proxyRule);
        handler.handle(request, response);

        verify(request).setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING, "mirrorServiceA");
    }

    @Test
    void should_not_mirror_request_if_percentage_zero() {
        when(request.getHeader(Constants.REQUEST_HEADER_USER_ID)).thenReturn("user123");

        mirroring.setMirrorPercentage(0);
        mirroring.setEnabled(true);

        MirroringHandler handler = new MirroringHandler(proxyRule);
        handler.handle(request, response);

        verify(request, never()).setAttribute(any(), any());
    }

    @Test
    void should_not_mirror_when_mirroring_not_enabled() {
        proxyRule.setMiddleware(new AppConfig.Middleware()); // no mirroring

        MirroringHandler handler = new MirroringHandler(proxyRule);
        handler.handle(request, response);

        verify(request, never()).setAttribute(any(), any());
    }

    @Test
    void should_fallback_to_session_id_if_user_id_header_missing() {
        mirroring.setMirrorPercentage(100); // force match
        mirroring.setEnabled(true);

        when(request.getHeader(Constants.REQUEST_HEADER_USER_ID)).thenReturn(null);

        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session-abc");
        when(request.getSession()).thenReturn(session);

        MirroringHandler handler = new MirroringHandler(proxyRule);
        handler.handle(request, response);

        verify(request).setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING, "mirrorServiceA");
    }
}
