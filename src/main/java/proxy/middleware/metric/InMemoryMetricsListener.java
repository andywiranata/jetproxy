package proxy.middleware.metric;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class InMemoryMetricsListener implements MetricsListener {
    private final ConcurrentHashMap<String, ConcurrentHashMap<LocalDateTime, MetricData>> metrics = new ConcurrentHashMap<>();

    @Data
    public static class MetricData {
        private final ConcurrentHashMap<Integer, AtomicInteger> statusCodeCounters = new ConcurrentHashMap<>();
        private final AtomicInteger hitCount = new AtomicInteger();

        public void increment(int statusCode) {
            statusCodeCounters.computeIfAbsent(statusCode, code -> new AtomicInteger()).incrementAndGet();
            hitCount.incrementAndGet();
        }

        public int getStatusCodeCount(int statusCode) {
            return statusCodeCounters.getOrDefault(statusCode, new AtomicInteger(0)).get();
        }

        public int getTotal() {
            return statusCodeCounters.values().stream().mapToInt(AtomicInteger::get).sum();
        }
    }

    @Override
    public void captureMetricProxyResponse(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        String queryParams = request.getQueryString();
        String fullPath = queryParams == null ? path : path + "?" + queryParams;
        int statusCode = response.getStatus();

        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        // Record metrics for the current date and hour
        metrics.computeIfAbsent(fullPath, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentHour, k -> new MetricData())
                .increment(statusCode);

        log.debug("Captured response for {} at {} -> Status: {}, Total Count: {}",
                fullPath, currentHour, statusCode, metrics.get(fullPath).get(currentHour).getTotal());
    }

    public Map<LocalDateTime, MetricData> getMetricsForPathLast24Hours(String path) {
        Map<LocalDateTime, MetricData> pathMetrics = metrics.get(path);
        if (pathMetrics == null) {
            return Map.of();
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        return pathMetrics.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(cutoffTime))
                .collect(ConcurrentHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), ConcurrentHashMap::putAll);
    }
}
