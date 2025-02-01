package io.jetproxy.middleware.handler;

import io.jetproxy.context.AppConfig;
import io.jetproxy.context.AppContext;
import io.jetproxy.service.holder.BaseProxyRequestHandler;
import io.jetproxy.util.Constants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.util.StringUtil;

import java.io.IOException;

public class GrpcRequestHandler  implements MiddlewareHandler{
    private final AppConfig.Proxy proxyRule;
    private final boolean isGrpcService;
    public GrpcRequestHandler(AppConfig.Proxy proxyRule, AppContext context) {
        this.proxyRule = proxyRule;
        this.isGrpcService = context.isUseGrpcService(proxyRule.getService());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!this.isGrpcService) {
            return;
        }
        String grpcServiceNameFromHeader = request.getHeader(Constants.REQUEST_HEADER_GRPC_SERVICE_NAME);
        String grpcMethodNameFromHeader = request.getHeader(Constants.REQUEST_HEADER_GRPC_METHOD_NAME);

        if (StringUtil.isBlank(grpcServiceNameFromHeader)
                && StringUtil.isBlank(grpcMethodNameFromHeader)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(Constants.HEADER_X_PROXY_ERROR, Constants.ERROR_METHOD_NOT_ALLOWED);
            response.setHeader(Constants.HEADER_X_PROXY_TYPE, Constants.TYPE_GRPC_SERV0CE_METHOD_NOT_FOUND);
            response.flushBuffer();
        } else {
            request.setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_GRPC_SERVICE_NAME, grpcServiceNameFromHeader);
            request.setAttribute(Constants.REQUEST_ATTRIBUTE_JETPROXY_GRPC_METHOD_NAME, grpcMethodNameFromHeader);
        }
    }
}
