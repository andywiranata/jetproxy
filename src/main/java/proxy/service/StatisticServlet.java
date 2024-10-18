package proxy.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import proxy.util.AbstractJsonServlet;

import java.io.IOException;

public class StatisticServlet extends AbstractJsonServlet<StatisticServlet.StatisticServletResponse> {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Create a response object
        StatisticServletResponse statisticServletResponse = new StatisticServletResponse("Your Stats Here");
        sendJsonResponse(resp, statisticServletResponse);
    }

    public static class StatisticServletResponse {
        private String status;

        public StatisticServletResponse(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}