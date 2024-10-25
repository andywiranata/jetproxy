package proxy.middleware.metric;

import com.timgroup.statsd.StatsDClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class StatsDMetricsListener implements MetricsListener {

    private final StatsDClient statsDClient;

    public StatsDMetricsListener(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    @Override
    public void captureMetricProxyResponse(HttpServletRequest request, HttpServletResponse response) {
        // HTTP Metadata
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryParams = request.getQueryString() != null ? request.getQueryString() : "";
        String host = request.getRemoteHost();
        int statusCode = response.getStatus();

        // Tags to collect as much metadata as possible
        Map<String, String> tags = new HashMap<>();
        tags.put("method", method);
        tags.put("path", path);
        tags.put("query_params", queryParams);
        tags.put("host", host);
        tags.put("status_code", String.valueOf(statusCode));

        // Send metric for hit count with tags
//        statsDClient.incrementCounter("http.request.hit_count", tags);
//
//        // Send metric for status code with tags
//        statsDClient.incrementCounter("http.request.status_code." + statusCode, tags);

        // Optionally: add latency metric if available
        // statsDClient.recordExecutionTime("http.request.latency", responseTime, tags);
    }
}
