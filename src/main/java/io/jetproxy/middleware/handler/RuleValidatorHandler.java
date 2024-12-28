package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.middleware.rule.RuleContext;
import io.jetproxy.middleware.rule.RuleFactory;
import io.jetproxy.service.holder.BaseProxyRequestHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RuleValidatorHandler implements MiddlewareHandler {
    protected RuleContext ruleContext;
    protected List<String > allowedMethods;

    public RuleValidatorHandler(AppConfig.Service configService,
                                AppConfig.Proxy proxyRule) {
        this.ruleContext = Optional.ofNullable(proxyRule.getMiddleware())
                .map(AppConfig.Middleware::getRule)
                .map(RuleFactory::createRulesFromString)
                .orElse(null);
        this.allowedMethods = configService.getMethods();
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (hasRuleContext() && !ruleContext.evaluate(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            response.setHeader(BaseProxyRequestHandler.HEADER_X_PROXY_ERROR,
                    BaseProxyRequestHandler.ERROR_RULE_NOT_ALLOWED);
            response.setHeader(BaseProxyRequestHandler.HEADER_X_PROXY_TYPE,
                    BaseProxyRequestHandler.TYPE_RULE_NOT_ALLOWED);
            response.flushBuffer();
        }
        if (isMethodNotAllowed(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.setHeader(BaseProxyRequestHandler.HEADER_X_PROXY_ERROR, BaseProxyRequestHandler.ERROR_METHOD_NOT_ALLOWED);
            response.setHeader(BaseProxyRequestHandler.HEADER_X_PROXY_TYPE, BaseProxyRequestHandler.TYPE_METHOD_NOT_ALLOWED);
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
