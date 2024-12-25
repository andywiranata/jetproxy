package io.jetproxy.service.holder.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// MiddlewareHandler.java
public interface MiddlewareHandler {
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
}
