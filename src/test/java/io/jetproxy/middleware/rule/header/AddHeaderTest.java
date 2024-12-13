package io.jetproxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import io.jetproxy.middleware.rule.RuleContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class AddHeaderTest {

    private HttpServletRequest mockRequest;
    private RuleContext mockRuleContext;

    @BeforeEach
    void setUp() {
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockRuleContext = Mockito.mock(TestRuleContext.class);
    }

    @Test
    void testExecute_AddHeaderWithoutRuleContext() {
        // Arrange
        String headerName = "X-Custom-Header";
        String headerValue = "CustomValue";
        AddHeader addHeader = new AddHeader(headerName, headerValue, null);
        Map<String, String> headers = new HashMap<>();

        // Act
        addHeader.execute(mockRequest, headers);

        // Assert
        assertEquals(1, headers.size());
        assertEquals(headerValue, headers.get(headerName));
    }

    @Test
    void testExecute_AddHeaderWithRuleContextTrue() {
        // Arrange
        String headerName = "X-Custom-Header";
        String headerValue = "CustomValue";
        when(mockRuleContext.evaluate(mockRequest)).thenReturn(true);
        AddHeader addHeader = new AddHeader(headerName, headerValue, mockRuleContext);
        Map<String, String> headers = new HashMap<>();

        // Act
        addHeader.execute(mockRequest, headers);

        // Assert
        assertEquals(1, headers.size());
        assertEquals(headerValue, headers.get(headerName));
    }

    @Test
    void testExecute_SkipHeaderWithRuleContextFalse() {
        // Arrange
        String headerName = "X-Custom-Header";
        String headerValue = "CustomValue";
        when(mockRuleContext.evaluate(mockRequest)).thenReturn(false);
        AddHeader addHeader = new AddHeader(headerName, headerValue, mockRuleContext);
        Map<String, String> headers = new HashMap<>();

        // Act
        addHeader.execute(mockRequest, headers);

        // Assert
        assertEquals(0, headers.size());
    }

    @Test
    void testExecute_WithoutHttpRequest_UsingServerHeaders() {
        // Arrange
        String headerName = "X-Server-Header";
        String headerValue = "ServerValue";
        AddHeader addHeader = new AddHeader(headerName, headerValue, null);
        Map<String, String> serverHeaders = new HashMap<>();
        Map<String, String> modifiedHeaders = new HashMap<>();

        // Act
        addHeader.execute(serverHeaders, modifiedHeaders);

        // Assert
        assertEquals(1, modifiedHeaders.size());
        assertEquals(headerValue, modifiedHeaders.get(headerName));
    }
}
