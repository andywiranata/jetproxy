package proxy.rule;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public class RuleContext {

    private final List<Rule> rules;
    private final List<String> operators;  // List of logical operators (&&, ||)

    public RuleContext(List<Rule> rules, List<String> operators) {
        this.rules = rules;
        this.operators = operators;
    }

    // Evaluates the rules against the request with AND/OR logic
    public boolean evaluate(HttpServletRequest request) {
        if (rules.isEmpty()) {
            return false;
        }

        boolean result = rules.get(0).evaluate(request);

        // Apply AND/OR logic to the rest of the rules
        for (int i = 1; i < rules.size(); i++) {
            String operator = operators.get(i - 1);
            boolean nextRuleResult = rules.get(i).evaluate(request);

            if ("&&".equals(operator)) {
                result = result && nextRuleResult;
            } else if ("||".equals(operator)) {
                result = result || nextRuleResult;
            }
        }

        return result;
    }
}
