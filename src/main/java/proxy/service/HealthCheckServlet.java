package proxy.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AbstractJsonServlet;

import java.io.IOException;

public class HealthCheckServlet extends AbstractJsonServlet<HealthCheckServlet.HealthCheckResponse> {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Create a response object
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse("UP");
        sendJsonResponse(resp, healthCheckResponse);
    }

    public static class HealthCheckResponse {
        private String status;

        public HealthCheckResponse(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}