package io.jetproxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import io.jetproxy.middleware.rule.RuleContext;

import java.util.Enumeration;
import java.util.Map;

public class AppendHeader implements HeaderAction {
    private final String pattern;       // The header pattern to match (e.g., "X-Custom-*")
    private final String valueToAppend; // The value to append to the header
    private final RuleContext ruleContext;

    public AppendHeader(String pattern, String valueToAppend, RuleContext ruleContext) {
        this.pattern = pattern;
        this.valueToAppend = valueToAppend;
        this.ruleContext = ruleContext;
    }

    @Override
    public void execute(HttpServletRequest request, Map<String, String> headers) {
        if (!shouldExecute(request, ruleContext)) {
            return; // Skip execution if rule context evaluation fails
        }

        // Iterate over request headers and apply the append logic
        Enumeration<String> requestHeaders = request.getHeaderNames();
        while (requestHeaders.hasMoreElements()) {
            String headerName = requestHeaders.nextElement();
            if (matchesWildcard(pattern, headerName)) {
                appendHeaderValue(headerName, request.getHeader(headerName), headers);
            }
        }
    }

    @Override
    public void execute(Map<String, String> serverHeaders, Map<String, String> modifiedHeaders) {
        for (Map.Entry<String, String> entry : serverHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (matchesWildcard(pattern, headerName)) {
                appendHeaderValue(headerName, entry.getValue(), modifiedHeaders);
            }
        }
    }

    /**
     * Appends the value to the header if it matches the pattern.
     *
     * @param headerName     The name of the header.
     * @param existingValue  The existing value of the header.
     * @param targetHeaders  The target map to store the modified headers.
     */
    private void appendHeaderValue(String headerName, String existingValue, Map<String, String> targetHeaders) {
        String newValue = (existingValue == null || existingValue.isEmpty())
                ? valueToAppend
                : existingValue + "," + valueToAppend;
        targetHeaders.put(headerName, newValue);
    }
}
