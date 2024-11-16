package proxy.middleware.rule.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class CopyHeaderTest {

    private HttpServletRequest request;
    private RuleContext ruleContext;
    private CopyHeader copyHeaderAction;
    private Map<String, String> headers;

    @BeforeEach
    public void setUp() {
        // Mock HttpServletRequest
        request = Mockito.mock(HttpServletRequest.class);
        ruleContext = Mockito.mock(RuleContext.class);

        // Initialize CopyHeader with source pattern and target prefix
        copyHeaderAction = new CopyHeader("X-Custom-*", "Copied-", ruleContext);

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
        copyHeaderAction.execute(request, headers);

        // Verify that the header was copied with the correct prefix
        assertEquals("some-value", headers.get("Copied-X-Custom-Header"));
    }

    @Test
    public void testExecuteRequestHeadersNotMatchingPattern() {
        // Simulate a request with a header that doesn't match the pattern
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Other-Header")));
        Mockito.when(request.getHeader("X-Other-Header")).thenReturn("some-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        copyHeaderAction.execute(request, headers);

        // Verify that no header was copied
        assertFalse(headers.containsKey("Copied-X-Other-Header"));
    }

    @Test
    public void testExecuteRequestWithNullValue() {
        // Simulate a request with a header that matches the pattern but has a null value
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Custom-Header")));
        Mockito.when(request.getHeader("X-Custom-Header")).thenReturn(null);

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Execute the action
        copyHeaderAction.execute(request, headers);

        // Verify that the header was not copied (because the value was null)
        assertFalse(headers.containsKey("Copied-X-Custom-Header"));
    }

    @Test
    public void testExecuteWithRuleContextSkippingExecution() {
        // Simulate the rule context returning false (skip execution)
        Mockito.when(ruleContext.evaluate(request)).thenReturn(false);

        // Execute the action
        copyHeaderAction.execute(request, headers);

        // Verify that no headers were copied
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testExecuteWithServerHeaders() {
        // Simulate server headers (no request object involved)
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", "some-value");

        // Execute the action
        copyHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was copied with the correct prefix
        assertEquals("some-value", headers.get("Copied-X-Custom-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNotMatchingPattern() {
        // Simulate server headers that don't match the pattern
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Other-Header", "some-value");

        // Execute the action
        copyHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was not copied
        assertFalse(headers.containsKey("Copied-X-Other-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNullValue() {
        // Simulate server headers with null values
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", null);

        // Execute the action
        copyHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was not copied
        assertFalse(headers.containsKey("Copied-X-Custom-Header"));
    }
}
