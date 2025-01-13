package io.jetproxy.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final String newRequestURI;

    public CustomHttpServletRequestWrapper(HttpServletRequest request, String newRequestURI) {
        super(request);
        this.newRequestURI = newRequestURI;
    }

    @Override
    public String getRequestURI() {
        return newRequestURI;
    }
}
