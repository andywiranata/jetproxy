package io.jetproxy.middleware.rule;

public enum RuleType {
    HEADER,          // Exact match for header value
    HEADER_PREFIX,   // Prefix match for header value
    HEADER_REGEX,    // Regex match for header value
    QUERY,           // Exact match for query parameter
    QUERY_PREFIX,    // Prefix match for query parameter
    QUERY_REGEX,     // Regex match for query parameter
    PATH,            // Exact match for path
    PATH_PREFIX,     // Prefix match for path
    PATH_REGEX,       // Regex match for path
    HOST,
    HOST_PREFIX,
    HOST_REGEX
}

