package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.middleware.rule.RuleContext;
import io.jetproxy.middleware.rule.RuleFactory;
import io.jetproxy.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RuleValidatorHandlerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void should_return_406_if_rule_fails() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        AppConfig.Middleware middleware = new AppConfig.Middleware();
        middleware.setRule("host:api.example.com");

        AppConfig.Proxy proxyRule = new AppConfig.Proxy();
        proxyRule.setMiddleware(middleware);

        RuleContext ruleContext = mock(RuleContext.class);
        when(ruleContext.evaluate(request)).thenReturn(false);

        try (MockedStatic<RuleFactory> staticMock = mockStatic(RuleFactory.class)) {
            staticMock.when(() -> RuleFactory.createRulesFromString("host:api.example.com")).thenReturn(ruleContext);

            StringWriter out = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(out));

            RuleValidatorHandler handler = new RuleValidatorHandler(List.of("GET", "POST"), proxyRule);
            handler.handle(request, response);

            verify(response).setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            verify(response).setHeader(Constants.HEADER_X_PROXY_ERROR, Constants.ERROR_RULE_NOT_ALLOWED);
            verify(response).setHeader(Constants.HEADER_X_PROXY_TYPE, Constants.TYPE_RULE_NOT_ALLOWED);
            verify(response).flushBuffer();
        }
    }

    @Test
    void should_return_405_if_method_not_allowed() throws Exception {
        when(request.getMethod()).thenReturn("PUT");

        AppConfig.Proxy proxyRule = new AppConfig.Proxy(); // No middleware or rule

        StringWriter out = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(out));

        RuleValidatorHandler handler = new RuleValidatorHandler(List.of("GET", "POST"), proxyRule);
        handler.handle(request, response);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response).setHeader(Constants.HEADER_X_PROXY_ERROR, Constants.ERROR_METHOD_NOT_ALLOWED);
        verify(response).setHeader(Constants.HEADER_X_PROXY_TYPE, Constants.TYPE_METHOD_NOT_ALLOWED);
        verify(response).flushBuffer();
    }

    @Test
    void should_skip_if_rule_passes_and_method_allowed() throws Exception {
        when(request.getMethod()).thenReturn("POST");

        AppConfig.Middleware middleware = new AppConfig.Middleware();
        middleware.setRule("host:example.com");

        AppConfig.Proxy proxyRule = new AppConfig.Proxy();
        proxyRule.setMiddleware(middleware);

        RuleContext ruleContext = mock(RuleContext.class);
        when(ruleContext.evaluate(request)).thenReturn(true);

        try (MockedStatic<RuleFactory> staticMock = mockStatic(RuleFactory.class)) {
            staticMock.when(() -> RuleFactory.createRulesFromString("host:example.com")).thenReturn(ruleContext);

            RuleValidatorHandler handler = new RuleValidatorHandler(List.of("POST"), proxyRule);
            handler.handle(request, response);

            verify(response, never()).setStatus(anyInt());
            verify(response, never()).flushBuffer();
        }
    }

    @Test
    void should_skip_if_no_rule_and_method_allowed() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        AppConfig.Proxy proxyRule = new AppConfig.Proxy(); // No middleware

        RuleValidatorHandler handler = new RuleValidatorHandler(List.of("GET", "POST"), proxyRule);
        handler.handle(request, response);

        verify(response, never()).setStatus(anyInt());
        verify(response, never()).flushBuffer();
    }
}
