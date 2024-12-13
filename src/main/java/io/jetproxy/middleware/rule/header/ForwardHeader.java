package io.jetproxy.middleware.rule.header;

import io.jetproxy.middleware.rule.RuleContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.Map;

public class ForwardHeader implements HeaderAction {
    private final String pattern;       // Pattern to match headers
    private final RuleContext ruleContext; // Optional rule context for conditional execution

    public ForwardHeader(String pattern, RuleContext ruleContext) {
        this.pattern = pattern;
        this.ruleContext = ruleContext;
    }

    @Override
    public void execute(HttpServletRequest request, Map<String, String> headers) {
        if (!shouldExecute(request, ruleContext)) {
            return; // Skip execution if the rule context evaluation fails
        }

        // Iterate over request headers and forward matching headers
        Enumeration<String> requestHeaders = request.getHeaderNames();
        while (requestHeaders.hasMoreElements()) {
            String headerName = requestHeaders.nextElement();
            if (matchesWildcard(pattern, headerName)) {
                forwardHeader(headerName, request.getHeader(headerName), headers);
            }
        }
    }

    @Override
    public void execute(Map<String, String> serverHeaders, Map<String, String> modifiedHeaders) {
        for (Map.Entry<String, String> entry : serverHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (matchesWildcard(pattern, headerName)) {
                forwardHeader(headerName, entry.getValue(), modifiedHeaders);
            }
        }
    }

    /**
     * Forwards the header by adding it to the target headers map.
     *
     * @param headerName   The name of the header.
     * @param headerValue  The value of the header.
     * @param targetHeaders The map to store forwarded headers.
     */
    private void forwardHeader(String headerName, String headerValue, Map<String, String> targetHeaders) {
        if (headerValue != null) {
            targetHeaders.put(headerName, headerValue);
        }
    }
    }
