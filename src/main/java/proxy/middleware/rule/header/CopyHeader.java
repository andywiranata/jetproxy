package proxy.middleware.rule.header;


import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Enumeration;
import java.util.Map;

public class CopyHeader implements HeaderAction {
    private final String sourcePattern;
    private final String targetPrefix;
    private final RuleContext ruleContext;

    public CopyHeader(String sourcePattern, String targetPrefix, RuleContext ruleContext) {
        this.sourcePattern = sourcePattern;
        this.targetPrefix = targetPrefix;
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
            if (matchesWildcard(sourcePattern, headerName)) {
                headers.put(targetPrefix + headerName, request.getHeader(headerName));
            }
        }
    }
}
