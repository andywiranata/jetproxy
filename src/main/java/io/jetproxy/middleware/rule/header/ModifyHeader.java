package io.jetproxy.middleware.rule.header;

import io.jetproxy.middleware.rule.RuleContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;

public class ModifyHeader implements HeaderAction {
    private final String pattern;                  // Pattern to match headers
    private final Function<String, String> modifier; // Function to modify header values
    private final RuleContext ruleContext;        // Optional rule context for conditional execution

    public ModifyHeader(String pattern, Function<String, String> modifier, RuleContext ruleContext) {
        this.pattern = pattern;
        this.modifier = modifier;
        this.ruleContext = ruleContext;
    }

    @Override
    public void execute(HttpServletRequest request, Map<String, String> headers) {
        if (!shouldExecute(request, ruleContext)) {
            return; // Skip execution if rule context evaluation fails
        }

        Enumeration<String> requestHeaders = request.getHeaderNames();
        while (requestHeaders.hasMoreElements()) {
            String headerName = requestHeaders.nextElement();
            if (matchesWildcard(pattern, headerName)) {
                modifyHeader(headerName, request.getHeader(headerName), headers);
            }
        }
    }

    @Override
    public void execute(Map<String, String> serverHeaders, Map<String, String> modifiedHeaders) {
        for (Map.Entry<String, String> entry : serverHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (matchesWildcard(pattern, headerName)) {
                modifyHeader(headerName, entry.getValue(), modifiedHeaders);
            }
        }
    }

    /**
     * Modifies the value of the header and stores it in the target headers map.
     *
     * @param headerName   The name of the header.
     * @param headerValue  The original value of the header.
     * @param targetHeaders The map to store the modified headers.
     */
    private void modifyHeader(String headerName, String headerValue, Map<String, String> targetHeaders) {
        if (headerValue != null) {
            targetHeaders.put(headerName, modifier.apply(headerValue));
        }
    }

}
