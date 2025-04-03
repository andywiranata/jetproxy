package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Objects;

import static io.jetproxy.util.Constants.REQUEST_HEADER_USER_ID;

public class MirroringHandler implements MiddlewareHandler{
    private final AppConfig.Proxy proxyRule;
    public MirroringHandler(AppConfig.Proxy proxyRule) {
        this.proxyRule = proxyRule;

    }
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response)  {
        if (proxyRule.hasMiddleware() && proxyRule.getMiddleware().hasMirroring()) {
            if (shouldMirrorRequest(request,
                    proxyRule.getMiddleware().getMirroring().getMirrorPercentage())) {
                request.setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_MIRRORING,
                        proxyRule.getMiddleware().getMirroring().getMirrorService());
            }
        }
    }

    private boolean shouldMirrorRequest(HttpServletRequest request, int mirrorPercentage) {
        String identifier = request.getHeader(REQUEST_HEADER_USER_ID); // Preferred
        HttpSession session = request.getSession();

        if (identifier == null) {
            identifier = session.getId();
        }
        // Ensure sessionId is not null for hashing
        String hashKey = Objects.requireNonNullElse(identifier, "default-session");

        // Compute hash and determine mirroring based on percentage
        int hash = Math.abs(hashKey.hashCode());
        return (hash % 100) < mirrorPercentage;
    }
}
