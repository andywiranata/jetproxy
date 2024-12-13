package io.jetproxy.middleware.rule.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import io.jetproxy.middleware.rule.RuleContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ForwardHeaderTest {

    private HttpServletRequest request;
    private RuleContext ruleContext;
    private ForwardHeader forwardHeaderAction;
    private Map<String, String> headers;

    @BeforeEach
    public void setUp() {
        // Mock HttpServletRequest
        request = Mockito.mock(HttpServletRequest.class);
        ruleContext = Mockito.mock(RuleContext.class);

        // Initialize ForwardHeader with pattern
        forwardHeaderAction = new ForwardHeader("X-Custom-*", ruleContext);

        // Initialize headers map
        headers = new HashMap<>();
    }

    @Test
    public void testExecuteRequestHeadersMatchingPattern() {
        // Simulate a request with headers
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Custom-Header")));
        Mockito.when(request.getHeader("X-Custom-Header")).thenReturn("some-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        forwardHeaderAction.execute(request, headers);

        // Verify that the header was forwarded
        assertEquals("some-value", headers.get("X-Custom-Header"));
    }

    @Test
    public void testExecuteRequestHeadersNotMatchingPattern() {
        // Simulate a request with a header that doesn't match the pattern
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Other-Header")));
        Mockito.when(request.getHeader("X-Other-Header")).thenReturn("some-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        forwardHeaderAction.execute(request, headers);

        // Verify that no header was forwarded
        assertFalse(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteRequestWithNullValue() {
        // Simulate a request with a header that matches the pattern but has a null value
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Custom-Header")));
        Mockito.when(request.getHeader("X-Custom-Header")).thenReturn(null);

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        forwardHeaderAction.execute(request, headers);

        // Verify that the header was not forwarded (because the value was null)
        assertFalse(headers.containsKey("X-Custom-Header"));
    }

    @Test
    public void testExecuteWithRuleContextSkippingExecution() {
        // Simulate the rule context returning false (skip execution)
        Mockito.when(ruleContext.evaluate(request)).thenReturn(false);

        // Execute the action
        forwardHeaderAction.execute(request, headers);

        // Verify that no headers were forwarded
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testExecuteWithServerHeaders() {
        // Simulate server headers (no request object involved)
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", "some-value");

        // Execute the action
        forwardHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was forwarded
        assertEquals("some-value", headers.get("X-Custom-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNotMatchingPattern() {
        // Simulate server headers that don't match the pattern
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Other-Header", "some-value");

        // Execute the action
        forwardHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was not forwarded
        assertFalse(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNullValue() {
        // Simulate server headers with null values
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", null);

        // Execute the action
        forwardHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was not forwarded
        assertFalse(headers.containsKey("X-Custom-Header"));
    }
}
