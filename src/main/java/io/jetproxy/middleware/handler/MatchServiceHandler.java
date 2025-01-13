package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.middleware.rule.RuleContext;
import io.jetproxy.middleware.rule.RuleFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

public class MatchServiceHandler implements MiddlewareHandler {
    public static final String JETPROXY_REWRITE_SERVICE = "jetproxy-rewrite-service";
    protected List<RuleContext> ruleContextList = new ArrayList<>();
    protected List<AppConfig.Match> matches = new ArrayList<>();
    public MatchServiceHandler(AppConfig.Proxy proxyRule) {
        this.matches = proxyRule.getMatches();
        for (AppConfig.Match match : proxyRule.getMatches()) {
            RuleContext ruleContext = Optional
                    .ofNullable(match.getRule())
                    .map(RuleFactory::createRulesFromString)
                    .orElse(null);
            ruleContextList.add(ruleContext);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        int index = 0;
        for (RuleContext ruleContext: ruleContextList){
            if (ruleContext.evaluate(request)) {
                request.setAttribute(JETPROXY_REWRITE_SERVICE,  this.matches.get(index).getService());
                return;
            }
            index++;
        }
    }
}
