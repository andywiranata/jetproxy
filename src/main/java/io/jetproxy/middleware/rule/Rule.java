package io.jetproxy.middleware.rule;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule {
    private final RuleType type;
    private final String target;    // e.g., "Content-Type" for Header, "id" for Query
    private final String value;     // e.g., the expected value or regex pattern

    public Rule(RuleType type, String target, String value) {
        this.type = type;
        this.target = target;
        this.value = value;
    }

    // Evaluates the rule based on the type and request data
    public boolean evaluate(HttpServletRequest request) {
        return switch (type) {
            case HEADER -> value.equals(request.getHeader(target));
            case HEADER_PREFIX -> request.getHeader(target) != null && request.getHeader(target).startsWith(value);
            case HEADER_REGEX -> matchRegex(request.getHeader(target), value);
            case QUERY -> value.equals(request.getParameter(target));
            case QUERY_PREFIX -> request.getParameter(target) != null && request.getParameter(target).startsWith(value);
            case QUERY_REGEX -> matchRegex(request.getParameter(target), value);
            case PATH -> value.equals(request.getRequestURI());
            case PATH_PREFIX -> request.getRequestURI().startsWith(value);
            case PATH_REGEX -> matchRegex(request.getRequestURI(), value);
            case HOST -> value.equals(request.getRemoteHost());
            case HOST_PREFIX -> request.getRemoteHost() != null && request.getRemoteHost().startsWith(value);
            case HOST_REGEX -> matchRegex(request.getRemoteHost(), value);
        };
    }

    // Helper method to evaluate regex matches
    private boolean matchRegex(String input, String regex) {
        if (input == null) return false;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }
}
