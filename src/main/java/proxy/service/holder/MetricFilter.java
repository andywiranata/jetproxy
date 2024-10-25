package proxy.service.holder;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MetricFilter implements Filter {

    public MetricFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String path = httpRequest.getRequestURI();
        String queryParams = httpRequest.getQueryString();
        String fullPath = queryParams == null ? path : path + "?" + queryParams;
        String host = httpRequest.getRemoteHost();

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);  // Pass wrapped response
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;

            // Get the response body content

        }
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}