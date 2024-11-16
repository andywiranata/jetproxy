package proxy.middleware.rule.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RemoveHeaderTest {

    private HttpServletRequest request;
    private RuleContext ruleContext;
    private RemoveHeader removeHeaderAction;
    private Map<String, String> headers;

    @BeforeEach
    public void setUp() {
        // Mock HttpServletRequest and RuleContext
        request = Mockito.mock(HttpServletRequest.class);
        ruleContext = Mockito.mock(RuleContext.class);

        // Initialize headers map
        headers = new HashMap<>();
    }

    @Test
    public void testExecuteRequestHeadersMatchingPattern() {
        // Add headers to the map
        headers.put("X-Custom-Header", "value");
        headers.put("X-Custom-Another-Header", "value");
        headers.put("X-Other-Header", "value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Initialize RemoveHeader with a pattern
        removeHeaderAction = new RemoveHeader("X-Custom-*", ruleContext);

        // Execute the action
        removeHeaderAction.execute(request, headers);

        // Verify that only matching headers are removed
        assertFalse(headers.containsKey("X-Custom-Header"));
        assertFalse(headers.containsKey("X-Custom-Another-Header"));
        assertTrue(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteRequestHeadersNotMatchingPattern() {
        // Add headers to the map
        headers.put("X-Other-Header", "value");
        headers.put("X-Another-Header", "value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Initialize RemoveHeader with a pattern
        removeHeaderAction = new RemoveHeader("X-Custom-*", ruleContext);

        // Execute the action
        removeHeaderAction.execute(request, headers);

        // Verify that no headers are removed
        assertTrue(headers.containsKey("X-Other-Header"));
        assertTrue(headers.containsKey("X-Another-Header"));
    }

    @Test
    public void testExecuteWithRuleContextSkippingExecution() {
        // Add headers to the map
        headers.put("X-Custom-Header", "value");
        headers.put("X-Custom-Another-Header", "value");

        // Simulate rule context evaluation returning false
        Mockito.when(ruleContext.evaluate(request)).thenReturn(false);

        // Initialize RemoveHeader with a pattern
        removeHeaderAction = new RemoveHeader("X-Custom-*", ruleContext);

        // Execute the action
        removeHeaderAction.execute(request, headers);

        // Verify that no headers are removed
        assertTrue(headers.containsKey("X-Custom-Header"));
        assertTrue(headers.containsKey("X-Custom-Another-Header"));
    }

    @Test
    public void testExecuteWithServerHeaders() {
        // Add headers to the serverHeaders map
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", "value");
        serverHeaders.put("X-Custom-Another-Header", "value");
        serverHeaders.put("X-Other-Header", "value");

        // Initialize RemoveHeader with a pattern
        removeHeaderAction = new RemoveHeader("X-Custom-*", null);

        // Execute the action
        removeHeaderAction.execute(serverHeaders, new HashMap<>()); // Empty map for modified headers

        // Verify that only matching headers are removed from serverHeaders
        assertFalse(serverHeaders.containsKey("X-Custom-Header"));
        assertFalse(serverHeaders.containsKey("X-Custom-Another-Header"));
        assertTrue(serverHeaders.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNotMatchingPattern() {
        // Add headers to the map
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Other-Header", "value");
        serverHeaders.put("X-Another-Header", "value");

        // Initialize RemoveHeader with a pattern
        removeHeaderAction = new RemoveHeader("X-Custom-*", null);

        // Execute the action
        removeHeaderAction.execute(serverHeaders, headers);

        // Verify that no headers are removed
        assertTrue(serverHeaders.containsKey("X-Other-Header"));
        assertTrue(serverHeaders.containsKey("X-Another-Header"));
    }
}
