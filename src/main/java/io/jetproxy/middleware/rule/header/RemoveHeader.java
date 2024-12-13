package io.jetproxy.middleware.rule.header;

import io.jetproxy.middleware.rule.RuleContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public class RemoveHeader implements HeaderAction {
    private final String pattern;       // Pattern to match headers for removal
    private final RuleContext ruleContext; // Optional rule context for conditional execution

    public RemoveHeader(String pattern, RuleContext ruleContext) {
        this.pattern = pattern;
        this.ruleContext = ruleContext;
    }

    @Override
    public void execute(HttpServletRequest request, Map<String, String> headers) {
        if (!shouldExecute(request, ruleContext)) {
            return; // Skip execution if rule context evaluation fails
        }

        // Remove matching headers from the map
        headers.keySet().removeIf(headerName -> matchesWildcard(pattern, headerName));
    }

    @Override
    public void execute(Map<String, String> serverHeaders, Map<String, String> modifiedHeaders) {
        // Debug log

        // Remove matching headers from the map
        serverHeaders.keySet().removeIf(headerName -> {
            return matchesWildcard(pattern, headerName);
        });

    }


}
