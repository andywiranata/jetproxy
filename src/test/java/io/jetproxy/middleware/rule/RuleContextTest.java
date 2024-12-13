package io.jetproxy.middleware.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class RuleContextTest {

    private HttpServletRequest request;
    private Rule ruleTrue;
    private Rule ruleFalse;

    @BeforeEach
    public void setUp() {
        // Mock HttpServletRequest
        request = Mockito.mock(HttpServletRequest.class);

        // Mock rules
        ruleTrue = Mockito.mock(Rule.class);
        ruleFalse = Mockito.mock(Rule.class);

        // Mock the evaluate method to return true or false based on the rule
        Mockito.when(ruleTrue.evaluate(request)).thenReturn(true);
        Mockito.when(ruleFalse.evaluate(request)).thenReturn(false);
    }

    @Test
    public void testEvaluateNoRules() {
        RuleContext ruleContext = new RuleContext(Arrays.asList(), Arrays.asList());
        boolean result = ruleContext.evaluate(request);
        assertTrue(result, "Evaluation with no rules should return true.");
    }

    @Test
    public void testEvaluateSingleTrueRule() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleTrue),
                Arrays.asList());
        boolean result = ruleContext.evaluate(request);
        assertTrue(result, "Evaluation with a single true rule should return true.");
    }

    @Test
    public void testEvaluateSingleFalseRule() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleFalse),
                Arrays.asList());
        boolean result = ruleContext.evaluate(request);
        assertFalse(result, "Evaluation with a single false rule should return false.");
    }

    @Test
    public void testEvaluateWithAndOperatorTrue() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleTrue, ruleTrue),
                Arrays.asList("&&"));
        boolean result = ruleContext.evaluate(request);
        assertTrue(result, "Evaluation with AND operator and all true rules should return true.");
    }

    @Test
    public void testEvaluateWithAndOperatorFalse() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleTrue, ruleFalse),
                Arrays.asList("&&"));
        boolean result = ruleContext.evaluate(request);
        assertFalse(result, "Evaluation with AND operator and one false rule should return false.");
    }

    @Test
    public void testEvaluateWithOrOperatorTrue() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleTrue, ruleFalse),
                Arrays.asList("||"));
        boolean result = ruleContext.evaluate(request);
        assertTrue(result, "Evaluation with OR operator and one true rule should return true.");
    }

    @Test
    public void testEvaluateWithOrOperatorFalse() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleFalse, ruleFalse),
                Arrays.asList("||"));
        boolean result = ruleContext.evaluate(request);
        assertFalse(result, "Evaluation with OR operator and all false rules should return false.");
    }

    @Test
    public void testEvaluateMultipleOperators() {
        RuleContext ruleContext = new RuleContext(
                Arrays.asList(ruleTrue, ruleFalse, ruleTrue),
                Arrays.asList("&&", "||"));
        boolean result = ruleContext.evaluate(request);
        assertTrue(result, "Evaluation with multiple operators should return true.");
    }
}
