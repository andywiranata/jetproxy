package io.jetproxy.middleware.cors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import java.io.IOException;

public class CorsHandlerWrapper extends HandlerWrapper {

    private final CorsProcessor corsProcessor;

    public CorsHandlerWrapper(CorsProcessor corsProcessor) {
        this.corsProcessor = corsProcessor;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        // Apply CORS Headers
        corsProcessor.applyCorsHeaders(request, response);

        // Handle OPTIONS pre-flight requests
        if (corsProcessor.handlePreFlight(request, response)) {
            baseRequest.setHandled(true);
            return; // Stop further processing
        }

        // Continue to the next handler
        super.handle(target, baseRequest, request, response);
    }
}
