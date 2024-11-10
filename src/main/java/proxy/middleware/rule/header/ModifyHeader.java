package proxy.middleware.rule.header;

import jakarta.servlet.http.HttpServletRequest;
import proxy.middleware.rule.RuleContext;

import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;

public class ModifyHeader implements HeaderAction {
    private final String pattern;
    private final Function<String, String> modifier;
    private final RuleContext ruleContext;

    public ModifyHeader(String pattern, Function<String, String> modifier, RuleContext ruleContext) {
        this.pattern = pattern;
        this.modifier = modifier;
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
                String value = request.getHeader(headerName);
                if (value != null) {
                    headers.put(headerName, modifier.apply(value));
                }
            }
        }
    }
}
