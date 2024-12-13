package io.jetproxy.middleware.rule.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import io.jetproxy.middleware.rule.RuleContext;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

public class AppendHeaderTest {

    private HttpServletRequest request;
    private RuleContext ruleContext;
    private AppendHeader appendHeaderAction;
    private Map<String, String> headers;

    @BeforeEach
    public void setUp() {
        // Mock HttpServletRequest
        request = Mockito.mock(HttpServletRequest.class);
        ruleContext = Mockito.mock(RuleContext.class);

        // Initialize AppendHeader with a pattern and value to append
        appendHeaderAction = new AppendHeader("X-Custom-*", "new-value", ruleContext);

        // Mock header names and values
        headers = new HashMap<>();
    }

    @Test
    public void testExecuteRequestHeadersMatchingPattern() {
        // Simulate a request with headers
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Custom-Header")));
        Mockito.when(request.getHeader("X-Custom-Header")).thenReturn("existing-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        appendHeaderAction.execute(request, headers);

        // Verify that the header was appended correctly
        assertEquals("existing-value,new-value", headers.get("X-Custom-Header"));
    }

    @Test
    public void testExecuteRequestHeadersNotMatchingPattern() {
        // Simulate a request with a header that doesn't match the pattern
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Other-Header")));
        Mockito.when(request.getHeader("X-Other-Header")).thenReturn("existing-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        appendHeaderAction.execute(request, headers);

        // Verify that no headers were added or modified
        assertFalse(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteRequestWithNoMatchingHeaders() {
        // Simulate a request with no headers matching the pattern
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Another-Header")));
        Mockito.when(request.getHeader("X-Another-Header")).thenReturn("value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        appendHeaderAction.execute(request, headers);

        // Verify that no headers were appended
        assertFalse(headers.containsKey("X-Another-Header"));
    }

    @Test
    public void testExecuteWithRuleContextSkippingExecution() {
        // Simulate the rule context returning false (skip execution)
        Mockito.when(ruleContext.evaluate(request)).thenReturn(false);

        // Execute the action
        appendHeaderAction.execute(request, headers);

        // Verify that no headers were added or modified
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testExecuteWithServerHeaders() {
        // Simulate server headers (no request object involved)
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", "existing-value");

        // Execute the action
        appendHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was appended correctly
        assertEquals("existing-value,new-value", headers.get("X-Custom-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNotMatchingPattern() {
        // Simulate server headers that don't match the pattern
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Other-Header", "existing-value");

        // Execute the action
        appendHeaderAction.execute(serverHeaders, headers);

        // Verify that the header wasn't added or modified
        assertFalse(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNoMatching() {
        // Simulate server headers that don't match the pattern
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Another-Header", "value");

        // Execute the action
        appendHeaderAction.execute(serverHeaders, headers);

        // Verify that no headers were appended
        assertFalse(headers.containsKey("X-Another-Header"));
    }
}
