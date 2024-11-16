package proxy.middleware.rule.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class ModifyHeaderTest {

    private HttpServletRequest request;
    private RuleContext ruleContext;
    private ModifyHeader modifyHeaderAction;
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
        // Simulate a request with headers
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Custom-Header")));
        Mockito.when(request.getHeader("X-Custom-Header")).thenReturn("original-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Define a modifier function to modify the header value
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader with the pattern and modifier
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, ruleContext);

        // Execute the action
        modifyHeaderAction.execute(request, headers);

        // Verify that the header was modified correctly
        assertEquals("original-value-modified", headers.get("X-Custom-Header"));
    }

    @Test
    public void testExecuteRequestHeadersNotMatchingPattern() {
        // Simulate a request with a header that doesn't match the pattern
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Other-Header")));
        Mockito.when(request.getHeader("X-Other-Header")).thenReturn("original-value");

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Define a modifier function
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, ruleContext);

        // Execute the action
        modifyHeaderAction.execute(request, headers);

        // Verify that no header was modified
        assertFalse(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteRequestWithNullValue() {
        // Simulate a request with a header that matches the pattern but has a null value
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Custom-Header")));
        Mockito.when(request.getHeader("X-Custom-Header")).thenReturn(null);

        // Simulate rule context evaluation returning true
        Mockito.when(ruleContext.evaluate(request)).thenReturn(true);

        // Define a modifier function
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, ruleContext);

        // Execute the action
        modifyHeaderAction.execute(request, headers);

        // Verify that the header was not modified
        assertFalse(headers.containsKey("X-Custom-Header"));
    }

    @Test
    public void testExecuteWithRuleContextSkippingExecution() {
        // Simulate the rule context returning false (skip execution)
        Mockito.when(ruleContext.evaluate(request)).thenReturn(false);

        // Define a modifier function
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, ruleContext);

        // Execute the action
        modifyHeaderAction.execute(request, headers);

        // Verify that no headers were modified
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testExecuteWithServerHeaders() {
        // Simulate server headers (no request object involved)
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", "original-value");

        // Define a modifier function
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, null);

        // Execute the action
        modifyHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was modified correctly
        assertEquals("original-value-modified", headers.get("X-Custom-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNotMatchingPattern() {
        // Simulate server headers that don't match the pattern
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Other-Header", "original-value");

        // Define a modifier function
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, null);

        // Execute the action
        modifyHeaderAction.execute(serverHeaders, headers);

        // Verify that no headers were modified
        assertFalse(headers.containsKey("X-Other-Header"));
    }

    @Test
    public void testExecuteWithServerHeadersNullValue() {
        // Simulate server headers with null values
        Map<String, String> serverHeaders = new HashMap<>();
        serverHeaders.put("X-Custom-Header", null);

        // Define a modifier function
        Function<String, String> modifier = value -> value + "-modified";

        // Initialize ModifyHeader
        modifyHeaderAction = new ModifyHeader("X-Custom-*", modifier, null);

        // Execute the action
        modifyHeaderAction.execute(serverHeaders, headers);

        // Verify that the header was not modified
        assertFalse(headers.containsKey("X-Custom-Header"));
    }
}
