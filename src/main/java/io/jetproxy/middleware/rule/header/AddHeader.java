package io.jetproxy.middleware.rule.header;

import io.jetproxy.middleware.rule.RuleContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public class AddHeader implements HeaderAction {
    private final String headerName;   // Header to be added
    private final String headerValue;  // Value for the header
    private final RuleContext ruleContext; // Optional rule context for conditional execution

    public AddHeader(String headerName, String headerValue, RuleContext ruleContext) {
        this.headerName = headerName;
        this.headerValue = headerValue;
        this.ruleContext = ruleContext;
    }

    @Override
    public void execute(HttpServletRequest request, Map<String, String> headers) {
        if (!shouldExecute(request, ruleContext)) {
            return; // Skip execution if rule context evaluation fails
        }
        headers.put(headerName, headerValue);
    }

    @Override
    public void execute(Map<String, String> serverHeaders, Map<String, String> modifiedHeaders) {
        modifiedHeaders.put(headerName, headerValue);
    }
}
