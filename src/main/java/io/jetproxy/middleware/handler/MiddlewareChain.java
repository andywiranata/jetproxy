package io.jetproxy.middleware.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;

import java.io.IOException;
import java.util.List;

public class MiddlewareChain {
    private final List<MiddlewareHandler> handlers;

    public MiddlewareChain(List<MiddlewareHandler> handlers) {
        this.handlers = handlers;
    }

    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        for (MiddlewareHandler handler : handlers) {
            handler.handle(request, response);
            if (response.isCommitted()) {
                return;
            }
        }
    }
    /** Called after backend response is received */
    public void postProcess(HttpServletRequest request, Response proxyResponse, byte[] buffer) {
        for (MiddlewareHandler handler : handlers) {
            handler.postHandle(request, proxyResponse, buffer);
        }
    }
}