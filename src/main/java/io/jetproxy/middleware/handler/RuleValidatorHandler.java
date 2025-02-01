package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.middleware.rule.RuleContext;
import io.jetproxy.middleware.rule.RuleFactory;
import io.jetproxy.service.holder.BaseProxyRequestHandler;
import io.jetproxy.util.Constants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RuleValidatorHandler implements MiddlewareHandler {
    protected RuleContext ruleContext;
    protected List<String > allowedMethods;

    public RuleValidatorHandler(List<String> httpMethods,
                                AppConfig.Proxy proxyRule) {
        this.ruleContext = Optional.ofNullable(proxyRule.getMiddleware())
                .map(AppConfig.Middleware::getRule)
                .map(RuleFactory::createRulesFromString)
                .orElse(null);
        this.allowedMethods = httpMethods;
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (hasRuleContext() && !ruleContext.evaluate(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            response.setHeader(Constants.HEADER_X_PROXY_ERROR,
                    Constants.ERROR_RULE_NOT_ALLOWED);
            response.setHeader(Constants.HEADER_X_PROXY_TYPE,
                    Constants.TYPE_RULE_NOT_ALLOWED);
            response.flushBuffer();
        }
        if (isMethodNotAllowed(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.setHeader(Constants.HEADER_X_PROXY_ERROR, Constants.ERROR_METHOD_NOT_ALLOWED);
            response.setHeader(Constants.HEADER_X_PROXY_TYPE, Constants.TYPE_METHOD_NOT_ALLOWED);
            response.flushBuffer();
        }

    }
    private boolean hasRuleContext() {
        return this.ruleContext != null;
    }
    public boolean isMethodNotAllowed(String requestMethod) {
        return !allowedMethods.contains(requestMethod);
    }
}
