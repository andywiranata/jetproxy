package proxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;
import java.util.Enumeration;
import java.util.Map;

public class ForwardHeader implements HeaderAction {
    private final String pattern;
    private final RuleContext ruleContext;

    public ForwardHeader(String pattern, RuleContext ruleContext) {
        this.pattern = pattern;
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
                headers.put(headerName, request.getHeader(headerName));
            }
        }
    }
}
