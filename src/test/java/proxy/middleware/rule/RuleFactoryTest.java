package proxy.middleware.rule;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

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
        String ruleString = "Header('Content-Type', 'application/json') && Path('api/v1')";

        // Act
        RuleContext ruleContext = RuleFactory.createRulesFromString(ruleString);

        // Assert
        assertNotNull(ruleContext);
    }
}
