package proxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Collections;

public class TestRuleContext extends RuleContext {
    public TestRuleContext() {
        super(Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public boolean evaluate(HttpServletRequest request) {
        return true; // Always return true for testing
    }
}
