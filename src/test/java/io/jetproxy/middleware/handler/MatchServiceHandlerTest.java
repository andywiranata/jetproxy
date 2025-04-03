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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchServiceHandlerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void should_set_service_when_rule_matches() {
        // Setup rule match config
        AppConfig.Match match = new AppConfig.Match();
        match.setService("mockService");
        match.setRule("path:/api");

        AppConfig.Proxy proxyRule = new AppConfig.Proxy();
        proxyRule.setMatches(List.of(match));

        RuleContext ruleContext = mock(RuleContext.class);
        when(ruleContext.evaluate(request)).thenReturn(true);

        // Static mock RuleFactory.createRulesFromString
        try (MockedStatic<RuleFactory> factory = mockStatic(RuleFactory.class)) {
            factory.when(() -> RuleFactory.createRulesFromString("path:/api")).thenReturn(ruleContext);

            MatchServiceHandler handler = new MatchServiceHandler(proxyRule);
            handler.handle(request, response);

            verify(request).setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE, "mockService");
        }
    }

    @Test
    void should_not_set_service_when_no_match() {
        AppConfig.Match match = new AppConfig.Match();
        match.setService("noMatchService");
        match.setRule("path:/api");

        AppConfig.Proxy proxyRule = new AppConfig.Proxy();
        proxyRule.setMatches(List.of(match));

        RuleContext ruleContext = mock(RuleContext.class);
        when(ruleContext.evaluate(request)).thenReturn(false);

        try (MockedStatic<RuleFactory> factory = mockStatic(RuleFactory.class)) {
            factory.when(() -> RuleFactory.createRulesFromString("path:/api")).thenReturn(ruleContext);

            MatchServiceHandler handler = new MatchServiceHandler(proxyRule);
            handler.handle(request, response);

            verify(request, never()).setAttribute(eq(Constants.REQUEST_ATTRIBUTE_JETPROXY_REWRITE_SERVICE), any());
        }
    }

    @Test
    void should_skip_if_no_match_rules_defined() {
        AppConfig.Proxy proxyRule = new AppConfig.Proxy(); // no match rules

        MatchServiceHandler handler = new MatchServiceHandler(proxyRule);
        handler.handle(request, response);

        verifyNoInteractions(request);
    }
}
