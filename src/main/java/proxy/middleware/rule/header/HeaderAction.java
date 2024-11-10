package proxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Map;

public interface HeaderAction {
    void execute(HttpServletRequest request, Map<String, String> headers);

    // Add RuleContext for conditional operations
    default boolean shouldExecute(HttpServletRequest request, RuleContext ruleContext) {
        return ruleContext == null || ruleContext.evaluate(request);
    }

    default boolean matchesWildcard(String pattern, String headerName) {
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return headerName.startsWith(prefix);
        }
        return headerName.equalsIgnoreCase(pattern);
    }
}