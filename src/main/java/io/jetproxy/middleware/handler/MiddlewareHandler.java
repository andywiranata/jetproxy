package io.jetproxy.middleware.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;

import java.io.IOException;

// MiddlewareHandler.java
public interface MiddlewareHandler {
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
    /** Called after response content is received */
    default void postHandle(HttpServletRequest request, Response proxyResponse, byte[] buffer) {}


}
