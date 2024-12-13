package io.jetproxy.middleware.rule;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.junit.jupiter.api.Assertions.*;

class RuleFactoryTest {

    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    void testCreateRulesFromString_withValidRuleString() {
        // Arrange
        String ruleString = "(Header('Content-Type', 'application/json') && HeaderPrefix('User-Agent', 'Mozilla')) || HeaderRegex('X-Custom-Header', '^[a-zA-Z0-9]{10}$')";

        // Act
        RuleContext ruleContext = RuleFactory.createRulesFromString(ruleString);

        // Assert
        assertNotNull(ruleContext);
    }
}
