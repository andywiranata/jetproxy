package proxy.service.holder;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class RuleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Continue the chain
        filterChain.doFilter(request, response);

    }

    private boolean evaluateRule(String rule, HttpServletRequest request) {
        // Simplified parsing for AND (&&) and OR (||) operations
        boolean headerMatches = false;
        boolean queryMatches = false;

        // Evaluate header() part of the rule
        if (rule.contains("header(")) {
            String[] headerParts = extractNameAndValue(rule, "header");
            String headerName = headerParts[0];
            String headerValue = headerParts[1];
            headerMatches = headerValue.equals(request.getHeader(headerName));
        }

        // Evaluate queryParam() part of the rule
        if (rule.contains("queryParam(")) {
            String[] queryParts = extractNameAndValue(rule, "queryParam");
            String paramName = queryParts[0];
            String paramValue = queryParts[1];
            queryMatches = paramValue.equals(request.getParameter(paramName));
        }

        // Determine if this is an AND (&&) or OR (||) rule
        if (rule.contains("&&")) {
            // AND condition
            return headerMatches && queryMatches;
        } else if (rule.contains("||")) {
            // OR condition
            return headerMatches || queryMatches;
        }

        // Default to false if no valid rule structure is found
        return false;
    }

    // Helper to extract the parameter names and values between parentheses in "header()" or "queryParam()"
    private String[] extractNameAndValue(String rule, String keyword) {
        int startIndex = rule.indexOf(keyword + "(") + keyword.length() + 1;
        int endIndex = rule.indexOf(")", startIndex);
        String inside = rule.substring(startIndex, endIndex);
        String[] parts = inside.split(",");
        return new String[]{parts[0].trim(), parts[1].trim()};  // Return both the name and value
    }

}
