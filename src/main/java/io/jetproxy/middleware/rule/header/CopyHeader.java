package io.jetproxy.middleware.rule.header;

import io.jetproxy.middleware.rule.RuleContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.Map;

public class CopyHeader implements HeaderAction {
    private final String sourcePattern;  // Pattern to match source headers
    private final String targetPrefix;   // Prefix to use for the copied headers
    private final RuleContext ruleContext;

    public CopyHeader(String sourcePattern, String targetPrefix, RuleContext ruleContext) {
        this.sourcePattern = sourcePattern;
        this.targetPrefix = targetPrefix;
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
            if (matchesWildcard(sourcePattern, headerName)) {
                copyHeader(headerName, request.getHeader(headerName), headers);
            }
        }
    }

    @Override
    public void execute(Map<String, String> serverHeaders, Map<String, String> modifiedHeaders) {
        for (Map.Entry<String, String> entry : serverHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (matchesWildcard(sourcePattern, headerName)) {
                copyHeader(headerName, entry.getValue(), modifiedHeaders);
            }
        }
    }

    /**
     * Copies the header value from the source to the target with the specified prefix.
     *
     * @param headerName  The name of the source header.
     * @param headerValue The value of the source header.
     * @param targetHeaders The map to store the modified headers.
     */
    public void copyHeader(String headerName, String headerValue, Map<String, String> targetHeaders) {
        if (headerValue != null) {
            targetHeaders.put(targetPrefix + headerName, headerValue);
        }
    }

}
