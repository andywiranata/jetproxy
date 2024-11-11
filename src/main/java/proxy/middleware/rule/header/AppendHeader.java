package proxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Enumeration;
import java.util.Map;

public class AppendHeader implements HeaderAction {
    private final String pattern;
    private final String valueToAppend;
    private final RuleContext ruleContext;

    public AppendHeader(String pattern, String valueToAppend, RuleContext ruleContext) {
        this.pattern = pattern;
        this.valueToAppend = valueToAppend;
        this.ruleContext = ruleContext;
    }

    @Override
    public void execute(HttpServletRequest request, Map<String, String> headers) {
        if (!shouldExecute(request, ruleContext)) {
            return;
        }

        Enumeration<String> requestHeaders = request.getHeaderNames();
        while (requestHeaders.hasMoreElements()) {
            String headerName = requestHeaders.nextElement();
            if (matchesWildcard(pattern, headerName)) {
                String existingValue = headers.getOrDefault(headerName, "");
                headers.put(headerName, existingValue.isEmpty() ? valueToAppend : existingValue + "," + valueToAppend);
            }
        }
    }
}